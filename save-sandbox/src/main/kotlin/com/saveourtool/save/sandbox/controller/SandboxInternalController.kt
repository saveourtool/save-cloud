package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.sandbox.service.BodilessResponseEntity
import com.saveourtool.save.sandbox.service.SandboxAgentRepository
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.utils.*

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.net.URI
import java.nio.ByteBuffer

import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.outputStream

typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>
typealias StringResponse = ResponseEntity<String>

/**
 * Sandbox implementation of endpoints which are required for save-agent
 */
@RestController
@RequestMapping("/sandbox/internal")
class SandboxInternalController(
    private val storage: Storage<SandboxStorageKey>,
    private val agentRepository: SandboxAgentRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * @param agentVersion
     * @return Mono with empty body
     */
    @PostMapping("/saveAgentVersion")
    fun saveAdditionalData(
        @RequestBody agentVersion: AgentVersion
    ): Mono<Unit> = {
        // Do nothing for now
    }.toMono()

    /**
     * @param testExecutionsDto
     * @return response with text value
     */
    @PostMapping("/saveTestResult")
    fun saveExecutionData(
        @RequestBody testExecutionsDto: List<TestExecutionDto>
    ): Mono<StringResponse> = ResponseEntity.ok("Do nothing for now")
        .toMono()

    /**
     * @param executionId
     * @return content of requested snapshot
     */
    @PostMapping(
        "/test-suites-sources/download-snapshot-by-execution-id",
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun downloadTestSourceSnapshot(
        @RequestParam executionId: Long
    ): Mono<ByteBufferFluxResponse> {
        val userName = agentRepository.getUserNameByExecutionId(executionId)
        val archiveFile = kotlin.io.path.createTempFile(
            prefix = "tests-",
            suffix = ARCHIVE_EXTENSION
        )
        return { createTempDirectory(prefix = "tests-directory-") }
            .toMono()
            .flatMap { directory ->
                storage.list()
                    .filter {
                        it.userName == userName && it.type == SandboxStorageKeyType.TEST
                    }
                    .flatMap { key ->
                        storage.download(key)
                            .mapToInputStream()
                            .map { inputStream ->
                                directory.resolve(key.fileName)
                                    .outputStream()
                                    .use {
                                        IOUtils.copy(inputStream, it)
                                    }
                            }
                    }
                    .collectList()
                    .map {
                        directory.compressAsZipTo(archiveFile)
                        FileUtils.deleteDirectory(directory.toFile())
                    }
            }
            .map {
                archiveFile.toByteBufferFlux()
            }
            .map {
                ResponseEntity.ok(it)
            }
            .doFinally {
                archiveFile.deleteExisting()
            }
    }

    /**
     * @param executionId
     * @param fileKey
     * @return content of requested file
     */
    @PostMapping("/files/download", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam executionId: Long,
        @RequestBody fileKey: FileKey,
    ): Mono<ByteBufferFluxResponse> {
        val userName = agentRepository.getUserNameByExecutionId(executionId)
        return storage.list()
            .filter { it.type == SandboxStorageKeyType.FILE && it.userName == userName && it.fileName == fileKey.name }
            .singleOrEmpty()
            .switchIfEmptyToNotFound {
                "No single file for $fileKey"
            }
            .map { key ->
                ResponseEntity.ok(storage.download(key))
            }
    }

    /**
     * @param version
     * @return content of requested save-cli
     */
    @PostMapping("/files/download-save-cli", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveCli(
        @RequestParam version: String,
    ): Mono<BodilessResponseEntity> =
            ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create("https://github.com/saveourtool/save-cli/releases/download/v$version/save-$version-linuxX64.kexe"))
                .build<Void>()
                .toMono()

    /**
     * @return content of save-agent
     */
    @PostMapping("/files/download-save-agent", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveAgent(): Mono<out Resource> =
            Mono.just(ClassPathResource("save-agent.kexe"))
                .filter { it.exists() }
                .switchIfEmptyToNotFound()

    /**
     * @param executionId
     * @param testResultDebugInfo
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping("/files/debug-info")
    fun saveDebugInfo(
        @RequestParam executionId: Long,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = blockingToMono {
        agentRepository.getUserNameByExecutionId(executionId)
    }
        .map { userName -> SandboxStorageKey.debugInfoKey(userName) }
        .flatMap { storageKey ->
            storage.overwrite(
                key = storageKey,
                content = testResultDebugInfo.toFluxByteBufferAsJson(objectMapper)
            )
        }
}
