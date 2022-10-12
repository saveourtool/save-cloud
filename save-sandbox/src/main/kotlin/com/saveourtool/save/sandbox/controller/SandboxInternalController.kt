package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.agent.AgentVersion
import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.sandbox.service.SandboxAgentRepository
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.sandbox.utils.userId
import com.saveourtool.save.utils.*

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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
    private val storage: SandboxStorage,
    private val agentRepository: SandboxAgentRepository,
    private val objectMapper: ObjectMapper,
) {
    /**
     * @param authentication
     * @return content of requested snapshot
     */
    @PostMapping(
        "/download-test-files",
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun downloadTestFiles(
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> {
        val archiveFile = kotlin.io.path.createTempFile(
            prefix = "tests-",
            suffix = ARCHIVE_EXTENSION
        )
        return { createTempDirectory(prefix = "tests-directory-") }
            .toMono()
            .flatMap { directory ->
                storage.list(authentication.userId(), SandboxStorageKeyType.TEST)
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
     * @param fileName
     * @param authentication
     * @return content of requested file
     */
    @PostMapping("/download-file", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        ResponseEntity.ok(
            storage.download(
                SandboxStorageKey(
                    userId = authentication.userId(),
                    type = SandboxStorageKeyType.FILE,
                    fileName = fileName,
                )
            )
        )
    }

    /**
     * @param version
     * @return content of requested save-cli
     */
    @PostMapping("/download-save-cli", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveCli(
        @RequestParam version: String,
    ): Mono<out Resource> =
            Mono.just(ClassPathResource("save-$version-linuxX64.kexe"))
                .filter { it.exists() }
                .switchIfEmptyToNotFound {
                    "Can't find save-$version-linuxX64.kexe with the requested version $version"
                }

    /**
     * @return content of save-agent
     */
    @PostMapping("/files/download-save-agent", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveAgent(): Mono<out Resource> =
            Mono.just(ClassPathResource("save-agent.kexe"))
                .filter { it.exists() }
                .switchIfEmptyToNotFound()

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
     * @param testResultDebugInfo
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping("/files/debug-info")
    fun uploadDebugInfo(
        @RequestParam executionId: Long,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = agentRepository.getUserIdAsMonoByExecutionId(executionId)
        .map { userId -> SandboxStorageKey.debugInfoKey(userId) }
        .flatMap { storageKey ->
            storage.overwrite(
                key = storageKey,
                content = testResultDebugInfo.toFluxByteBufferAsJson(objectMapper)
            )
        }
}
