package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxUserRepository
import com.saveourtool.save.sandbox.service.BodilessResponseEntity
import com.saveourtool.save.sandbox.service.SandboxAgentRepository
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
 * @property agentRepository
 */
@RestController
@RequestMapping("/sandbox/api")
class SandboxController(
    val configProperties: ConfigProperties,
    val storage: Storage<SandboxStorageKey>,
    val sandboxExecutionRepository: SandboxExecutionRepository,
    val sandboxUserRepository: SandboxUserRepository,
    val agentsController: AgentsController,
    val agentRepository: SandboxAgentRepository,
) {
    /**
     * @param userId
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart userId: Long,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userId,
        SandboxStorageKeyType.FILE,
        file
    )

    /**
     * @param userId
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-test", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTest(
        @RequestPart userId: Long,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userId,
        SandboxStorageKeyType.TEST,
        file
    )

    /**
     * @param userId
     * @param file
     * @return count of written bytes
     */
    @PostMapping("/upload-test-resource", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadTestResource(
        @RequestPart userId: Long,
        @RequestPart file: Mono<FilePart>,
    ): Mono<Long> = doUpload(
        userId,
        SandboxStorageKeyType.TEST_RESOURCE,
        file
    )

    private fun doUpload(
        userId: Long,
        type: SandboxStorageKeyType,
        file: Mono<FilePart>,
    ): Mono<Long> = file.flatMap { filePart ->
        storage.overwrite(
            key = SandboxStorageKey(
                userId,
                type,
                filePart.filename()
            ),
            content = filePart
        )
    }

    /**
     * @param userId
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @PostMapping(path = ["/get-debug-info"])
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
    }
        .map { execution ->
            agentRepository.getRunRequest(execution)
        }
        .flatMap { request ->
            agentsController.initialize(request)
        }
}
