package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.agent.TestExecutionResult
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.utils.*

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.nio.ByteBuffer

import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.outputStream

typealias ByteBufferFluxResponse = ResponseEntity<Flux<ByteBuffer>>

/**
 * Sandbox implementation of endpoints which are required for save-agent
 */
@RestController
@RequestMapping("/sandbox/internal")
class SandboxInternalController(
    private val storage: SandboxStorage,
    private val objectMapper: ObjectMapper,
) {
    /**
     * @param userId ID of User
     * @return content of requested snapshot
     */
    @PostMapping(
        "/download-test-files",
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE]
    )
    fun downloadTestFiles(
        @RequestParam userId: Long,
    ): Mono<ByteBufferFluxResponse> {
        val archiveFile = createTempFile(
            prefix = "tests-",
            suffix = ARCHIVE_EXTENSION
        )
        return { createTempDirectory(prefix = "tests-directory-") }
            .toMono()
            .flatMap { directory ->
                storage.list(userId, SandboxStorageKeyType.TEST)
                    .flatMap { key ->
                        storage.download(key)
                            .collectToInputStream()
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
     * @param userId ID of User
     * @param fileName
     * @return content of requested file
     */
    @PostMapping("/download-file", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam userId: Long,
        @RequestParam fileName: String,
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        ResponseEntity.ok(
            storage.download(
                SandboxStorageKey(
                    userId = userId,
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
    @RequestMapping(
        path = ["/download-save-cli"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        method = [RequestMethod.GET, RequestMethod.POST]
    )
    fun downloadSaveCli(
        @RequestParam version: String,
    ): Mono<out Resource> =
            run {
                val executable = "save-$version-linuxX64.kexe"

                downloadFromClasspath(executable) {
                    "Can't find $executable with the requested version $version"
                }
            }

    /**
     * @return content of save-agent
     */
    @RequestMapping(
        path = ["/download-save-agent"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        method = [RequestMethod.GET, RequestMethod.POST]
    )
    fun downloadSaveAgent(): Mono<out Resource> =
            run {
                val executable = "save-agent.kexe"

                downloadFromClasspath(executable) {
                    "Can't find $executable"
                }
            }

    /**
     * @param testExecutionResults
     * @return response with text value
     */
    @PostMapping("/upload-execution-data")
    fun saveExecutionData(
        @RequestBody testExecutionResults: List<TestExecutionResult>
    ): Mono<StringResponse> = ResponseEntity.ok("Do nothing for now")
        .toMono()

    /**
     * @param userId
     * @param testResultDebugInfo
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping("/upload-debug-info")
    fun uploadDebugInfo(
        @RequestParam userId: Long,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = storage.overwrite(
        key = SandboxStorageKey.debugInfoKey(userId),
        contentBytes = objectMapper.writeValueAsBytes(testResultDebugInfo)
    )
}
