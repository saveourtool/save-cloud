package com.saveourtool.save.sandbox.controller

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.authservice.utils.username
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.domain.SandboxFileInfo
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.controller.AgentsController
import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.sandbox.repository.SandboxAgentRepository
import com.saveourtool.save.sandbox.repository.SandboxAgentStatusRepository
import com.saveourtool.save.sandbox.repository.SandboxExecutionRepository
import com.saveourtool.save.sandbox.repository.SandboxLnkExecutionAgentRepository
import com.saveourtool.save.sandbox.service.SandboxOrchestratorAgentService
import com.saveourtool.save.sandbox.storage.SandboxStorage
import com.saveourtool.save.sandbox.storage.SandboxStorageKey
import com.saveourtool.save.sandbox.storage.SandboxStorageKeyType
import com.saveourtool.save.service.LogService
import com.saveourtool.save.utils.*

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.intellij.lang.annotations.Language
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
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
 * @property sandboxAgentRepository
 * @property sandboxAgentStatusRepository
 * @property sandboxLnkExecutionAgentRepository
 * @property agentsController
 * @property orchestratorAgentService
 * @property logService
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "sandbox"),
)
@RestController
@RequestMapping("/sandbox/api")
@SuppressWarnings("LongParameterList")
class SandboxController(
    val configProperties: ConfigProperties,
    val storage: SandboxStorage,
    val sandboxExecutionRepository: SandboxExecutionRepository,
    val sandboxAgentRepository: SandboxAgentRepository,
    val sandboxAgentStatusRepository: SandboxAgentStatusRepository,
    val sandboxLnkExecutionAgentRepository: SandboxLnkExecutionAgentRepository,
    val agentsController: AgentsController,
    val orchestratorAgentService: SandboxOrchestratorAgentService,
    val logService: LogService,
) {
    @Operation(
        method = "GET",
        summary = "Get a list of files for provided user",
        description = "Get a list of files for provided user",
    )
    @ApiResponse(responseCode = "200", description = "A list of files")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/list-file")
    fun listFiles(
        authentication: Authentication,
    ): Flux<SandboxFileInfo> = Mono.just(authentication.userId())
        .flatMapMany { userId ->
            storage.list(userId, SandboxStorageKeyType.FILE)
        }
        .flatMap {
            Mono.zip(
                it.toMono(),
                storage.usingProjectReactor().contentLength(it),
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
        Parameter(name = FILE_PART_NAME, `in` = ParameterIn.DEFAULT, description = "a file which needs to be uploaded", required = true),
        Parameter(name = CONTENT_LENGTH_CUSTOM, `in` = ParameterIn.HEADER, description = "size in bytes of a file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFile(
        @RequestPart file: Mono<FilePart>,
        @RequestHeader(CONTENT_LENGTH_CUSTOM) contentLength: Long,
        authentication: Authentication,
    ): Mono<SandboxFileInfo> = file.flatMap { filePart ->
        getAsMonoStorageKey(authentication.userId(), SandboxStorageKeyType.FILE, filePart.filename())
            .flatMap { key ->
                storage.usingProjectReactor().overwrite(
                    key = key,
                    content = filePart,
                    contentLength = contentLength,
                )
            }
            .map {
                SandboxFileInfo(filePart.filename(), contentLength)
            }
    }

    @Operation(
        method = "POST",
        summary = "Upload a file as text for provided user with provide file name",
        description = "Upload a file as text for provided user with provide file name",
    )
    @Parameters(
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "a content of an uploading file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-file-as-text")
    fun uploadFileAsText(
        @RequestParam fileName: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<SandboxFileInfo> = doUploadAsText(authentication.userId(), SandboxStorageKeyType.FILE, fileName, content)

    @Operation(
        method = "GET",
        summary = "Get a file for provided user with requested file name",
        description = "Get a file for provided user with requested file name",
    )
    @Parameters(
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Contest of a requested file")
    @ApiResponse(responseCode = "404", description = "User with such name or file with such file name and user was not found")
    @GetMapping("/download-file", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Flux<ByteBuffer> = getAsMonoStorageKey(authentication.userId(), SandboxStorageKeyType.FILE, fileName)
        .flatMapMany {
            storage.usingProjectReactor().download(it)
        }
        .switchIfEmptyToNotFound {
            "There is no file $fileName for user ${authentication.username()}"
        }

    @Operation(
        method = "GET",
        summary = "Download a file as text for provided user and requested file name",
        description = "Download a file as text for provided user and requested file name",
    )
    @Parameters(
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Content of the file as text")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/download-file-as-text")
    fun downloadFileAsText(
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Mono<String> = doDownloadAsText(authentication.userId(), SandboxStorageKeyType.FILE, fileName)

    @Operation(
        method = "DELETE",
        summary = "Delete a file for provided user with requested file name",
        description = "Delete a file for provided user with requested file name",
    )
    @Parameters(
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Result of delete operation of a requested file")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @DeleteMapping("/delete-file")
    fun deleteFile(
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Mono<Boolean> = getAsMonoStorageKey(authentication.userId(), SandboxStorageKeyType.FILE, fileName)
        .flatMap {
            storage.usingProjectReactor().delete(it)
        }

    @Operation(
        method = "POST",
        summary = "Upload a test file as text for provided user with provide file name",
        description = "Upload a test file as text for provided user with provide file name",
    )
    @Parameters(
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
        Parameter(name = "content", `in` = ParameterIn.DEFAULT, description = "a content of an uploading file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Uploaded bytes")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @PostMapping("/upload-test-as-text")
    fun uploadTestAsText(
        @RequestParam fileName: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<SandboxFileInfo> = doUploadAsText(authentication.userId(), SandboxStorageKeyType.TEST, fileName, content)

    private fun doUploadAsText(
        userId: Long,
        type: SandboxStorageKeyType,
        fileName: String,
        content: String,
    ): Mono<SandboxFileInfo> = getAsMonoStorageKey(userId, type, fileName)
        .flatMap { key ->
            storage.usingProjectReactor().overwrite(
                key = key,
                contentBytes = content.replace("\r\n?".toRegex(), "\n").toByteArray(),
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
        Parameter(name = "fileName", `in` = ParameterIn.QUERY, description = "file name", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Content of the test file as text")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping("/download-test-as-text")
    fun downloadTestAsText(
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Mono<String> = doDownloadAsText(authentication.userId(), SandboxStorageKeyType.TEST, fileName)

    private fun doDownloadAsText(
        userId: Long,
        type: SandboxStorageKeyType,
        fileName: String,
    ): Mono<String> = getAsMonoStorageKey(userId, type, fileName)
        .flatMap { key ->
            storage.usingProjectReactor().download(key)
                .collectToInputStream()
                .map { it.bufferedReader().readText() }
        }
        .switchIfEmpty(
            examples[fileName].toMono()
                .flatMap { example ->
                    doUploadAsText(userId, type, fileName, example)
                        .thenReturn(example)
                }
                .switchIfEmptyToNotFound {
                    "There is no test file $fileName for user id $userId"
                }
        )

    private fun getAsMonoStorageKey(
        userId: Long,
        type: SandboxStorageKeyType,
        fileName: String,
    ): Mono<SandboxStorageKey> = Mono.just(userId)
        .map { uid ->
            SandboxStorageKey(
                uid,
                type,
                fileName,
            )
        }

    @Operation(
        method = "GET",
        summary = "Download a debug info for provided user",
        description = "Download a debug info for provided user",
    )
    @ApiResponse(responseCode = "200", description = "Content of the debug info")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @GetMapping(path = ["/get-debug-info"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun getDebugInfo(
        authentication: Authentication,
    ): Flux<ByteBuffer> = Mono.just(authentication.userId())
        .flatMapMany { userId ->
            storage.usingProjectReactor().download(SandboxStorageKey.debugInfoKey(userId))
        }
        .switchIfEmptyToNotFound {
            "There is no DebugInfo for ${authentication.username()}"
        }

    @Operation(
        method = "POST",
        summary = "Run a new execution for provided user",
        description = "Run a new execution for provided user",
    )
    @Parameters(
        Parameter(name = "sdk", `in` = ParameterIn.QUERY, description = "SDK", required = true),
    )
    @ApiResponse(responseCode = "200", description = "empty response for execution run")
    @ApiResponse(responseCode = "404", description = "User with such name was not found")
    @ApiResponse(responseCode = "409", description = "There is a running execution")
    @PostMapping("/run-execution")
    @Transactional
    fun runExecution(
        @RequestParam sdk: String,
        authentication: Authentication,
    ): Mono<EmptyResponse> {
        val userId = authentication.userId()
        return validateNoRunningExecution(userId)
            .flatMap {
                storage.usingProjectReactor().delete(SandboxStorageKey.debugInfoKey(userId))
            }
            .map {
                SandboxExecution(
                    startTime = LocalDateTime.now(),
                    endTime = null,
                    status = ExecutionStatus.PENDING,
                    sdk = sdk,
                    userId = userId,
                    initialized = false,
                    failReason = null,
                )
            }
            .map { sandboxExecutionRepository.save(it) }
            .map { orchestratorAgentService.getRunRequest(it) }
            .flatMap { request ->
                agentsController.initialize(request)
            }
    }

    /**
     * @param limit
     * @param authentication
     * @return logs from agent sandbox
     */
    @GetMapping("/logs-from-agent")
    fun getAgentLogs(
        @RequestParam(required = false, defaultValue = "1000") limit: Int,
        authentication: Authentication,
    ): Mono<StringList> = blockingToMono {
        sandboxExecutionRepository.findTopByUserIdOrderByStartTimeDesc(authentication.userId())
    }
        .switchIfEmptyToNotFound {
            "There is no run for ${authentication.username()} yet"
        }
        .flatMap { execution ->
            val agent = sandboxLnkExecutionAgentRepository.findByExecutionId(execution.requiredId())
                .singleOrNull()
                .orConflict { "Only a single agent expected for execution ${execution.requiredId()}" }
                .agent
            val startTime = sandboxAgentStatusRepository.findTopByAgentOrderByStartTimeAsc(agent)
                ?.startTime
                .orNotFound { "Not found first agent status for execution ${execution.requiredId()}" }
            val endTime = sandboxAgentStatusRepository.findTopByAgentOrderByEndTimeDesc(agent)
                ?.endTime
                .orNotFound { "Not found latest agent status for execution ${execution.requiredId()}" }
            logService.getByContainerName(agent.containerName,
                startTime.toInstantAtDefaultZone(),
                endTime.toInstantAtDefaultZone(),
                limit,
            )
        }

    private fun validateNoRunningExecution(
        userId: Long,
    ): Mono<Unit> = blockingToMono {
        sandboxExecutionRepository.findByUserId(userId)
            .filter { it.status in setOf(ExecutionStatus.RUNNING, ExecutionStatus.PENDING) }
    }
        .requireOrSwitchToResponseException({ isEmpty() }, HttpStatus.CONFLICT) {
            "There is already a running execution"
        }
        .thenReturn(Unit)

    companion object {
        @Language("toml")
        private val saveTomlExample = """
            |# special configuration file to use save test framework: https://github.com/saveourtool/save-cli
            |[general]
            |execCmd = "python -m pylint"
            |# === example of expected tests CHECK-MESSAGES: :[[@LINE-1]]:12: warning: test [warning-name] ===
            |expectedWarningsPattern = "# CHECK-MESSAGES:?\\[\\[(.+)\\]\\]: (.*)"
            |
            |tags = ["check_only", "clang_tidy", "huawei_specific"]
            |description = "Demo suite of Huawei specific tests for Clang tidy"
            |suiteName = "Fixbot codecheck-python tests"
            |
            |
            |[warn]
            |actualWarningsPattern=".*[\\\\\\/](.*):(\\d+): (.*)"
            |
            |execFlags = "--msg-template=\"{path}:{line}: {msg_id}: {msg} ({symbol})\""
            |linePlaceholder = "@LINE"
            |
            |exactWarningsMatch = false
            |partialWarnTextMatch = true
            |testNameRegex = ".*test.*"
            |#testNameRegex = ".*.py"
            |warningTextHasColumn = false
            |warningTextHasLine = true
            |
            |lineCaptureGroup = 1
            |#columnCaptureGroup = null
            |messageCaptureGroup = 2
            |
            |
            |fileNameCaptureGroupOut = 1
            |lineCaptureGroupOut = 2
            |#columnCaptureGroupOut = null
            |messageCaptureGroupOut = 3
        """.trimMargin()

        @Language("python")
        private val testExample = """
            |# CHECK-MESSAGES:[[1]]: C0114: Missing module docstring (missing-module-docstring)
            |import tkinter as tk
            |
            |root=tk.Tk()
            |
            |canvas1 = tk.Canvas(root, width = 300, height = 300)
            |canvas1.pack()
            |
            |# CHECK-MESSAGES:[[10]]: C0116: Missing function or method docstring (missing-function-docstring)
            |def hello ():
            |    label1 = tk.Label(root, text= 'Hello World!', fg='blue', font=('helvetica', 12, 'bold'))
            |    canvas1.create_window(150, 200, window=label1)
            |
            |button1 = tk.Button(text='Click Me', command=hello, bg='brown',fg='white')
            |canvas1.create_window(150, 150, window=button1)
            |
            |root.mainloop()
        """.trimMargin()

        @Language("bash")
        private val setupShExample = """
            |#!/usr/bin/env bash
            |
            |# Here you can add some additional commands required to run your tool e.g.
            |# setup pylint, it setups all required dependencies
            |pip install pylint --no-input --disable-pip-version-check --no-warn-script-location
        """.trimMargin()
        private val examples = mapOf(
            "test" to testExample,
            "save.toml" to saveTomlExample,
            "setup.sh" to setupShExample,
        )
    }
}
