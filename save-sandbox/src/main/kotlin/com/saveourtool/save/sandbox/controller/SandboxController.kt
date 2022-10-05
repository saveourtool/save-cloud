package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxUserRepository
import com.saveourtool.save.sandbox.service.BodilessResponseEntity
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.mapToInputStream
import com.saveourtool.save.utils.overwrite
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.LocalDateTime
import javax.transaction.Transactional

/**
 * @property configProperties
 * @property storage
 * @property sandboxExecutionRepository
 * @property sandboxUserRepository
 * @property agentsController
 */
@RestController
@RequestMapping("/sandbox/api")
class SandboxController(
    val configProperties: ConfigProperties,
    val storage: SandboxStorage,
    val sandboxExecutionRepository: SandboxExecutionRepository,
    val sandboxUserRepository: SandboxUserRepository,
    val agentsController: AgentsController,
) {
    /**
     * @param userName
     * @return list of available files for provided [userName]
     */
    @GetMapping("/list-file")
    fun listFiles(
        @RequestParam userName: String,
    ): Flux<String> = blockingToMono { sandboxUserRepository.getIdByName(userName) }
        .flatMapMany { userId -> storage.list(userId, SandboxStorageKeyType.FILE) }
        .map { it.fileName }

    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = file.flatMap { filePart ->
        getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, filePart.filename())
            .flatMap { key ->
                storage.overwrite(
                    key = key,
                    content = filePart
                )
            }
    }

    /**
     * @param userName
     * @param fileName
     * @return count of written bytes
     */
    @GetMapping("/download-file", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Flux<ByteBuffer> = getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, fileName)
        .flatMapMany {
            storage.download(it)
        }

    /**
     * @param userName
     * @param fileName
     * @return result of delete operation
     */
    @DeleteMapping("/delete-file")
    fun deleteFile(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<Boolean> = getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, fileName)
        .flatMap {
            storage.delete(it)
        }

    /**
     * @param userName
     * @param fileName
     * @param content
     * @return count of written bytes
     */
    @PostMapping("/upload-test-as-text")
    fun uploadTestAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
        @RequestBody content: String,
    ): Mono<Long> = doUploadAsText(userName, SandboxStorageKeyType.TEST, fileName, content)

    /**
     * @param userName
     * @param fileName
     * @param content
     * @return count of written bytes
     */
    @PostMapping("/upload-test-resource-as-text")
    fun uploadTestResourceAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
        @RequestBody content: String,
    ): Mono<Long> = doUploadAsText(userName, SandboxStorageKeyType.TEST_RESOURCE, fileName, content)

    private fun doUploadAsText(
        userName: String,
        type: SandboxStorageKeyType,
        fileName: String,
        content: String,
    ): Mono<Long> = getAsMonoStorageKey(userName, type, fileName)
        .flatMap { key ->
            storage.overwrite(
                key = key,
                content = Flux.just(ByteBuffer.wrap(content.toByteArray()))
            )
        }

    /**
     * @param userName
     * @param fileName
     * @return content as text
     */
    @GetMapping("/download-test-as-text")
    fun downloadTestAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<String> = doDownloadAsText(userName, SandboxStorageKeyType.TEST, fileName)

    /**
     * @param userName
     * @param fileName
     * @return content as text
     */
    @GetMapping("/download-test-resource-as-text")
    fun downloadTestResourceAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<String> = doDownloadAsText(userName, SandboxStorageKeyType.TEST_RESOURCE, fileName)

    private fun doDownloadAsText(
        @RequestParam userName: String,
        @RequestParam type: SandboxStorageKeyType,
        @RequestParam fileName: String,
    ): Mono<String> = getAsMonoStorageKey(userName, type, fileName)
        .flatMap { key ->
            storage.download(key)
                .mapToInputStream()
                .map { it.bufferedReader().readText() }
        }
        .switchIfEmptyToNotFound {
            "There is no file $fileName ($type) for user $userName"
        }

    private fun getAsMonoStorageKey(
        userName: String,
        type: SandboxStorageKeyType,
        fileName: String,
    ): Mono<SandboxStorageKey> = blockingToMono { sandboxUserRepository.getIdByName(userName) }
        .map { userId ->
            SandboxStorageKey(
                userId,
                type,
                fileName,
            )
        }

    /**
     * @param userId
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @GetMapping(path = ["/get-debug-info"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getDebugInfo(
        @RequestParam userId: Long,
    ): Flux<ByteBuffer> = storage.download(SandboxStorageKey.debugInfoKey(userId))

    /**
     * @param userName
     * @param sdk
     * @return empty response
     */
    @PostMapping("/run-execution")
    @Transactional
    fun runExecution(
        @RequestParam userName: String,
        @RequestParam sdk: String,
    ): Mono<BodilessResponseEntity> = blockingToMono {
        val execution = SandboxExecution(
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.PENDING,
            sdk = sdk,
            userId = sandboxUserRepository.getIdByName(userName),
            initialized = false,
            failReason = null,
        )
        sandboxExecutionRepository.save(execution)
    }.map { execution ->
        execution.toRunRequest()
    }.flatMap { request ->
        agentsController.initialize(request)
    }
}
