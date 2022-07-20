package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.entities.Agent
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.orchestrator.BodilessResponseEntity
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import com.saveourtool.save.orchestrator.service.imageName
import com.saveourtool.save.utils.STANDARD_TEST_SUITE_DIR
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.warn

import com.github.dockerjava.api.exception.DockerClientException
import com.github.dockerjava.api.exception.DockerException
import io.fabric8.kubernetes.client.KubernetesClientException
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.doOnError
import reactor.kotlin.core.publisher.toFlux

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.outputStream

/**
 * Controller used to start agents with needed information
 */
@RestController
class AgentsController(
    private val agentService: AgentService,
    private val dockerService: DockerService,
    private val configProperties: ConfigProperties,
    @Qualifier("webClientBackend")
    private val webClientBackend: WebClient,
) {
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
            log.info(
                "Starting preparations for launching execution [project=${execution.project}, id=${execution.id}, " +
                        "status=${execution.status}, resourcesRootPath=${execution.resourcesRootPath}]"
            )
            getTestRootPath(execution)
                .switchIfEmpty(
                    Mono.error(
                        ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Failed detect testRootPath for execution.id = ${execution.requiredId()}"
                        )
                    )
                )
                .flatMap { testRootPath ->
                    val filesLocation = Paths.get(
                        configProperties.testResources.basePath,
                        execution.resourcesRootPath!!,
                        testRootPath
                    )
                    execution.parseAndGetAdditionalFiles()
                        .toFlux()
                        .flatMap { fileKey ->
                            val pathToFile = filesLocation.resolve(fileKey.name)
                            (fileKey to execution).downloadTo(pathToFile)
                                .map { unzipIfRequired(pathToFile) }
                        }
                        .collectList()
                        .switchIfEmpty(Mono.just(emptyList()))
                }
                .map {
                    // todo: pass SDK via request body
                    dockerService.buildBaseImage(execution)
                }
                .onErrorResume({ it is DockerException || it is DockerClientException }) { dex ->
                    reportExecutionError(execution, "Unable to build image and containers", dex)
                }
                .map { (baseImageId, agentRunCmd) ->
                    dockerService.createContainers(execution.id!!, baseImageId, agentRunCmd)
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
                .subscribeOn(agentService.scheduler)
                .subscribe()
        }
    }

    private fun getTestRootPath(execution: Execution): Mono<String> = when (execution.type) {
        ExecutionType.STANDARD -> Mono.just(STANDARD_TEST_SUITE_DIR)
        ExecutionType.GIT -> webClientBackend.post()
            .uri("/findTestRootPathForExecutionByTestSuites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(execution))
            .retrieve()
            .bodyToMono<List<String>>()
            .map { it.distinct().single() }
        else -> throw NotImplementedError("Not supported executionType ${execution.type}")
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
            pathToFile.unzipHere()
            if (shouldBeExecutable) {
                log.info { "Marking files in ${pathToFile.parent} executable..." }
                Files.walk(pathToFile.parent)
                    .filter { it.isRegularFile() }
                    .forEach { it.tryMarkAsExecutable() }
            }
            Files.delete(pathToFile)
        }
    }

    private fun Path.unzipHere() {
        log.debug { "Unzip ${this.absolutePathString()} into ${this.parent.absolutePathString()}" }
        try {
            val zipFile = ZipFile(this.toString())
            zipFile.extractAll(this.parent.toString())
        } catch (e: ZipException) {
            log.error("Error occurred during extracting of archive ${this.name}", e)
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
            .let {
                pathToFile.parent.createDirectories()
                DataBufferUtils.write(it, pathToFile.outputStream())
            }
            .map { DataBufferUtils.release(it) }
            .then(
                Mono.fromCallable {
                    // TODO: need to store information about isExecutable in Execution (FileKey)
                    pathToFile.tryMarkAsExecutable()
                    log.debug {
                        "Downloaded $fileKey to ${pathToFile.absolutePathString()}"
                    }
                }
            )
    }

    private fun Path.tryMarkAsExecutable() {
        try {
            Files.setPosixFilePermissions(this, Files.getPosixFilePermissions(this) + allExecute)
        } catch (ex: UnsupportedOperationException) {
            log.warn(ex) { "Failed to mark file ${this.name} as executable" }
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
        private val allExecute = setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE)
    }
}
