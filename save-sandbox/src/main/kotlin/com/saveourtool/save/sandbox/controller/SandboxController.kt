package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.domain.SandboxFileInfo
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
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
@ApiSwaggerSupport
@Tags(
    Tag(name = "sandbox"),
)
@RestController
@RequestMapping("/sandbox/api")
class SandboxController(
    val configProperties: ConfigProperties,
    val storage: SandboxStorage,
    val sandboxExecutionRepository: SandboxExecutionRepository,
    val sandboxUserRepository: SandboxUserRepository,
    val agentsController: AgentsController,
) {
    @Operation(
        method = "GET",
        summary = "Get a list of files for provided user",
        description = "Get a list of files for provided user",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "A list of files")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/list-file")
    fun listFiles(
        @RequestParam userName: String,
    ): Flux<SandboxFileInfo> = blockingToMono { sandboxUserRepository.getIdByName(userName) }
        .flatMapMany { userId ->
            storage.list(userId, SandboxStorageKeyType.FILE)
        }
        .flatMap {
            Flux.zip(
                it.toMono(),
                storage.contentSize(it),
            )
        }
        .map { (storageKey, size) ->
            SandboxFileInfo(storageKey.fileName, size)
        }

    @Operation(
        method = "POST",
        summary = "Upload a file for provided user",
        description = "Upload a file for provided user",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "file", `in` = ParameterIn.DEFAULT, description = "a file which needs to be uploaded", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestParam userName: String,
        @RequestPart file: Mono<FilePart>,
    ): Mono<SandboxFileInfo> = file.flatMap { filePart ->
        getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, filePart.filename())
            .flatMap { key ->
                storage.overwrite(
                    key = key,
                    content = filePart
                )
            }
            .map {
                SandboxFileInfo(filePart.filename(), it)
            }
    }

    @Operation(
        method = "POST",
        summary = "Upload a file as text for provided user with provide file name",
        description = "Upload a file as text for provided user with provide file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "a content of an uploading file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-file-as-text")
    fun uploadFileAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
        @RequestBody content: String,
    ): Mono<SandboxFileInfo> = doUploadAsText(userName, SandboxStorageKeyType.FILE, fileName, content)

    @Operation(
        method = "GET",
        summary = "Get a file for provided user with requested file name",
        description = "Get a file for provided user with requested file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Contest of a requested file")
    @ApiResponse(responseCode = "404", description = "User with such name or file with such file name and user was not found")
    @GetMapping("/download-file", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Flux<ByteBuffer> = getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, fileName)
        .flatMapMany {
            storage.download(it)
        }
        .switchIfEmptyToNotFound {
            "There is no file $fileName for user $userName"
        }

    @Operation(
        method = "GET",
        summary = "Download a file as text for provided user and requested file name",
        description = "Download a file as text for provided user and requested file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Content of the file as text")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/download-file-as-text")
    fun downloadFileAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<String> = doDownloadTestAsText(userName, SandboxStorageKeyType.FILE, fileName)

    @Operation(
        method = "DELETE",
        summary = "Delete a file for provided user with requested file name",
        description = "Delete a file for provided user with requested file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Result of delete operation of a requested file")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @DeleteMapping("/delete-file")
    fun deleteFile(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<Boolean> = getAsMonoStorageKey(userName, SandboxStorageKeyType.FILE, fileName)
        .flatMap {
            storage.delete(it)
        }

    @Operation(
        method = "POST",
        summary = "Upload a test file as text for provided user with provide file name",
        description = "Upload a test file as text for provided user with provide file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "a content of an uploading file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-test-as-text")
    fun uploadTestAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
        @RequestBody content: String,
    ): Mono<SandboxFileInfo> = doUploadAsText(userName, SandboxStorageKeyType.TEST, fileName, content)

    @Operation(
        method = "POST",
        summary = "Upload a test resource file as text for provided user with provide file name",
        description = "Upload a test resource file as text for provided user with provide file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "a content of an uploading file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-test-resource-as-text")
    fun uploadTestResourceAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
        @RequestBody content: String,
    ): Mono<SandboxFileInfo> = doUploadAsText(userName, SandboxStorageKeyType.TEST_RESOURCE, fileName, content)

    private fun doUploadAsText(
        userName: String,
        type: SandboxStorageKeyType,
        fileName: String,
        content: String,
    ): Mono<SandboxFileInfo> = getAsMonoStorageKey(userName, type, fileName)
        .flatMap { key ->
            storage.overwrite(
                key = key,
                content = Flux.just(ByteBuffer.wrap(content.toByteArray()))
            )
        }
        .map {
            SandboxFileInfo(fileName, it)
        }

    @Operation(
        method = "GET",
        summary = "Download a test file as text for provided user and requested file name",
        description = "Download a test file as text for provided user and requested file name",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Content of the test file as text")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/download-test-as-text")
    fun downloadTestAsText(
        @RequestParam userName: String,
        @RequestParam fileName: String,
    ): Mono<String> = doDownloadTestAsText(userName, SandboxStorageKeyType.TEST, fileName)

    private fun doDownloadTestAsText(
        userName: String,
        type: SandboxStorageKeyType,
        fileName: String,
    ): Mono<String> = getAsMonoStorageKey(userName, type, fileName)
        .flatMap { key ->
            storage.download(key)
                .mapToInputStream()
                .map { it.bufferedReader().readText() }
        }
        .switchIfEmptyToNotFound {
            "There is no test file $fileName for user $userName"
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

    @Operation(
        method = "GET",
        summary = "Download a debug info for provided user",
        description = "Download a debug info for provided user",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Content of the debug info")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping(path = ["/get-debug-info"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getDebugInfo(
        @RequestParam userName: String,
    ): Flux<ByteBuffer> = blockingToMono { sandboxUserRepository.getIdByName(userName) }
        .flatMapMany { userId ->
            storage.download(SandboxStorageKey.debugInfoKey(userId))
        }

    @Operation(
        method = "POST",
        summary = "Run a new execution for provided user",
        description = "Run a new execution for provided user",
    )
    @Parameters(
        Parameter(name = "userName", `in` = ParameterIn.QUERY, description = "user name", required = true),
        Parameter(name = "sdk", `in` = ParameterIn.QUERY, description = "SDK", required = true),
    )
    @ApiResponse(responseCode = "200", description = "empty response for execution run")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
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
