package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxUserRepository
import com.saveourtool.save.sandbox.service.BodilessResponseEntity
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.overwrite
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
    val storage: Storage<SandboxStorageKey>,
    val sandboxExecutionRepository: SandboxExecutionRepository,
    val sandboxUserRepository: SandboxUserRepository,
    val agentsController: AgentsController,
) {
    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userName,
        SandboxStorageKeyType.FILE,
        file
    )

    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-test", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTest(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userName,
        SandboxStorageKeyType.TEST,
        file
    )

    /**
     * @param userName
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-test-resource", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTestResource(
        @RequestPart userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userName,
        SandboxStorageKeyType.TEST_RESOURCE,
        file
    )

    private fun doUpload(
        userName: String,
        type: SandboxStorageKeyType,
        file: Mono<FilePart>,
    ): Mono<Long> = file.flatMap { filePart ->
        storage.overwrite(
            key = SandboxStorageKey(
                userName,
                type,
                filePart.filename()
            ),
            content = filePart
        )
    }

    /**
     * @param userName
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @PostMapping(path = ["/get-debug-info"])
    fun getDebugInfo(
        @RequestParam userName: String,
    ): Flux<ByteBuffer> = storage.download(SandboxStorageKey.debugInfoKey(userName))

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
            failReason = null,
        )
        sandboxExecutionRepository.save(execution)
    }.map { execution ->
        execution.toRunRequest(sandboxUserRepository::getNameById)
    }.flatMap { request ->
        agentsController.initialize(request)
    }


}
