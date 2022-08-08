package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.runner.TEST_SUITES_DIR_NAME
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.imageName
import com.saveourtool.save.orchestrator.utils.LoggingContextImpl
import com.saveourtool.save.orchestrator.utils.allExecute
import com.saveourtool.save.orchestrator.utils.tryMarkAsExecutable
import com.saveourtool.save.utils.*

import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.exception.DockerException
import io.fabric8.kubernetes.client.KubernetesClientException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.doOnError
import reactor.kotlin.core.publisher.toFlux

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.*

/**
 * Controller used to start agents with needed information
 */
@RestController
class AgentsController(
    private val agentService: AgentService,
    private val dockerService: DockerService,
    private val configProperties: ConfigProperties,
    @Qualifier("webClientBackend") private val webClientBackend: WebClient,
) {
    private val tmpDir = Paths.get(configProperties.testResources.tmpPath).createDirectories()

    /**
     * Schedules tasks to build base images, create a number of containers and put their data into the database.
     *
     * @param execution
     * @return OK if everything went fine.
     * @throws ResponseStatusException
     */
    @Suppress("TOO_LONG_FUNCTION", "LongMethod", "UnsafeCallOnNullableType")
    @PostMapping("/initializeAgents")
    fun initialize(@RequestPart(required = true) execution: Execution): Mono<BodilessResponseEntity> {
        if (execution.status != ExecutionStatus.PENDING) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Execution status must be PENDING"
            )
        }
        val response = Mono.just(ResponseEntity<Void>(HttpStatus.ACCEPTED))
            .subscribeOn(agentService.scheduler)
        return response.doOnSuccess {
            log.info {
                "Starting preparations for launching execution [project=${execution.project}, id=${execution.id}, " +
                        "status=${execution.status}]"
            }
            Mono.fromCallable {
                createTempDirectory(
                    directory = tmpDir,
                    prefix = "save-execution-${execution.id}"
                )
            }
                .flatMap { resourcesForExecution ->
                    val resourcesPath = resourcesForExecution.resolve(TEST_SUITES_DIR_NAME)
                    execution.downloadTestsTo(resourcesPath)
                        .then(execution.downloadAdditionalFilesTo(resourcesPath))
                        .thenReturn(resourcesForExecution)
                }
                .publishOn(agentService.scheduler)
                .map {
                    // todo: pass SDK via request body
                    dockerService.prepareConfiguration(it, execution)
                }
                .onErrorResume({ it is DockerException || it is DockerClientException }) { dex ->
                    reportExecutionError(execution, "Unable to build image and containers", dex)
                }
                .publishOn(agentService.scheduler)
                .map { configuration ->
                    dockerService.createContainers(execution.id!!, configuration)
                }
                .onErrorResume({ it is DockerException || it is KubernetesClientException }) { ex ->
                    reportExecutionError(execution, "Unable to create docker containers", ex)
                }
                .flatMap { agentIds ->
                    agentService.saveAgentsWithInitialStatuses(
                        agentIds.map { id ->
                            Agent(id, execution)
                        }
                    )
                        .doOnError(WebClientResponseException::class) { exception ->
                            log.error("Unable to save agents, backend returned code ${exception.statusCode}", exception)
                            dockerService.cleanup(execution.id!!)
                        }
                        .doOnSuccess {
                            dockerService.startContainersAndUpdateExecution(execution, agentIds)
                        }
                }
                .subscribe()
        }
    }

    // if some additional file is archive, unzip it into proper destination:
    // for standard mode into STANDARD_TEST_SUITE_DIR
    // for Git mode into testRootPath
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    private fun unzipIfRequired(
        pathToFile: Path,
    ) {
        // FixMe: for now support only .zip files
        if (pathToFile.name.endsWith(".zip")) {
            val shouldBeExecutable = Files.getPosixFilePermissions(pathToFile).any { allExecute.contains(it) }
            pathToFile.extractZipHereSafely()
            if (shouldBeExecutable) {
                log.info { "Marking files in ${pathToFile.parent} executable..." }
                Files.walk(pathToFile.parent)
                    .filter { it.isRegularFile() }
                    .forEach { with(loggingContext) { it.tryMarkAsExecutable() } }
            }
            Files.delete(pathToFile)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun Path.extractZipHereSafely() {
        try {
            extractZipHere()
        } catch (e: Exception) {
            log.error("Error occurred during extracting of archive $name", e)
        }
    }

    private fun Execution.downloadAdditionalFilesTo(
        targetDirectory: Path
    ): Mono<Unit> = parseAndGetAdditionalFiles()
        .toFlux()
        .flatMap { fileKey ->
            val pathToFile = targetDirectory.resolve(fileKey.name)
            (fileKey to this).downloadTo(pathToFile)
                .map { unzipIfRequired(pathToFile) }
        }
        .collectList()
        .map {
            log.info { "Downloaded all additional files for execution $id to $targetDirectory" }
        }
        .lazyDefaultIfEmpty {
            log.warn {
                "Not found any additional files for execution $id"
            }
        }

    private fun Pair<FileKey, Execution>.downloadTo(
        pathToFile: Path
    ): Mono<Unit> = this.let { (fileKey, execution) ->
        webClientBackend.post()
            .uri("/files/{organizationName}/{projectName}/download", execution.project.organization.name, execution.project.name)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fileKey)
            .accept(MediaType.APPLICATION_OCTET_STREAM)
            .retrieve()
            .bodyToFlux<DataBuffer>()
            .let { content ->
                pathToFile.parent.createDirectories()
                content.writeTo(pathToFile)
            }
            .then(
                Mono.fromCallable {
                    // TODO: need to store information about isExecutable in Execution (FileKey)
                    with(loggingContext) { pathToFile.tryMarkAsExecutable() }
                    log.debug {
                        "Downloaded $fileKey to ${pathToFile.absolutePathString()}"
                    }
                }
            )
            .lazyDefaultIfEmpty {
                log.warn {
                    "Not found additional file $fileKey for execution ${execution.id}"
                }
            }
    }

    private fun Execution.downloadTestsTo(
        targetDirectory: Path
    ): Mono<Unit> = webClientBackend.post()
        .uri(
            "/test-suites-sources/download-snapshot-by-execution-id?executionId={executionId}",
            requiredId(),
        )
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_OCTET_STREAM)
        .retrieve()
        .bodyToFlux<DataBuffer>()
        .let { content ->
            targetDirectory.createDirectories()
            val targetFile = Files.createTempFile(targetDirectory, "archive-", ARCHIVE_EXTENSION)
            content.writeTo(targetFile)
        }
        .map {
            it.extractZipHere()
            it.deleteExisting()
        }
        .map {
            log.info { "Downloaded all tests for execution $id to $targetDirectory" }
        }
        .lazyDefaultIfEmpty {
            log.warn {
                "Not found any tests for execution ${requiredId()}"
            }
        }

    private fun <T> reportExecutionError(
        execution: Execution,
        failReason: String,
        ex: Throwable?
    ): Mono<T> {
        log.error("$failReason for executionId=${execution.id}, will mark it as ERROR", ex)
        return execution.id?.let {
            agentService.updateExecution(it, ExecutionStatus.ERROR, failReason).then(Mono.empty())
        } ?: Mono.empty()
    }

    /**
     * @param agentIds list of IDs of agents to stop
     */
    @PostMapping("/stopAgents")
    fun stopAgents(@RequestBody agentIds: List<String>) {
        dockerService.stopAgents(agentIds)
    }

    /**
     * @param executionLogs ExecutionLogs
     */
    @PostMapping("/executionLogs", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun saveAgentsLog(@RequestPart(required = true) executionLogs: FilePart) {
        // File name is equals to agent id
        val agentId = executionLogs.filename()
        val logDir = File(configProperties.executionLogs)
        if (!logDir.exists()) {
            log.info("Folder to store logs from agents was created: ${logDir.name}")
            logDir.mkdirs()
        }
        val logFile = File(logDir.path + File.separator + "$agentId.log")
        if (!logFile.exists()) {
            logFile.createNewFile()
            log.info("Log file for $agentId agent was created")
        }
        executionLogs.content()
            .map { dtBuffer ->
                FileOutputStream(logFile, true).use { os ->
                    dtBuffer.asInputStream().use {
                        it.copyTo(os)
                    }
                }
            }
            .collectList()
            .doOnSuccess {
                log.info("Logs of agent with id = $agentId were written")
            }
            .subscribe()
    }

    /**
     * Delete containers and images associated with execution [executionId]
     *
     * @param executionId id of execution
     * @return empty response
     */
    @PostMapping("/cleanup")
    fun cleanup(@RequestParam executionId: Long) = Mono.fromCallable {
        dockerService.cleanup(executionId)
    }
        .doOnSuccess {
            dockerService.removeImage(imageName(executionId))
        }
        .flatMap {
            Mono.just(ResponseEntity<Void>(HttpStatus.OK))
        }

    companion object {
        private val log = LoggerFactory.getLogger(AgentsController::class.java)
        private val loggingContext = LoggingContextImpl(log)
    }
}
