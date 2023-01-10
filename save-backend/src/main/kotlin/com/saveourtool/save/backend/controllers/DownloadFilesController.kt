package com.saveourtool.save.backend.controllers

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.storage.*
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.File
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.from
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.ByteBuffer

typealias FileDtoResponse = ResponseEntity<FileDto>

/**
 * A Spring controller for file downloading
 */
@RestController
@ApiSwaggerSupport
@Tags(
    Tag(name = "files"),
)
@Suppress("LongParameterList")
class DownloadFilesController(
    private val avatarStorage: AvatarStorage,
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val agentService: AgentService,
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
    private val projectService: ProjectService,
    private val fileService: FileService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return a list of files in [fileStorage]
     */
    @GetMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/list"])
    @Suppress("UnsafeCallOnNullableType")
    fun list(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Flux<FileDto> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.READ
    )
        .flatMapIterable {
            fileService.getByProject(it)
        }

    /**
     * @param fileId
     * @param authentication
     * @return [Mono] with response
     */
    @DeleteMapping(path = ["/api/$v1/files/delete"])
    @Suppress("UnsafeCallOnNullableType")
    fun delete(
        @RequestParam fileId: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        fileService.get(fileId)
    }
        .zipWhen { file ->
            with(projectPermissionEvaluator) {
                Mono.just(file.project)
                    .filterByPermission(authentication, Permission.DELETE, HttpStatus.FORBIDDEN)
            }
        }
        .flatMap { (file, _) ->
            val fileDto = file.toDto()
            fileStorage.delete(fileDto)
                .asyncEffectIf({ this }) {
                    blockingToMono {
                        fileService.delete(fileDto)
                    }
                }
        }
        .map { deleted ->
            if (deleted) {
                ResponseEntity.ok("File deleted successfully")
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found by id $fileId")
            }
        }

    @Operation(
        method = "POST",
        summary = "Download a file by execution ID and FileKey.",
        description = "Download a file by execution ID and FileKey.",
    )
    @Parameters(
        Parameter(name = "fileId", `in` = ParameterIn.QUERY, description = "ID of additional file", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @ApiResponse(responseCode = "404", description = "File with provided ID is not found.")
    @PostMapping(path = ["/api/$v1/files/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(
        @RequestParam fileId: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        fileService.get(fileId)
    }
        .zipWhen { file ->
            with(projectPermissionEvaluator) {
                Mono.just(file.project)
                    .filterByPermission(authentication, Permission.READ, HttpStatus.FORBIDDEN)
            }
        }
        .flatMap { (file, _) ->
            doDownload(file)
        }

    /**
     * @param fileId
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/internal/files/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun internalDownload(
        @RequestParam fileId: Long,
    ): Mono<ByteBufferFluxResponse> = doDownload(fileService.get(fileId))

    private fun doDownload(file: File): Mono<ByteBufferFluxResponse> = doDownload(file.toDto())

    private fun doDownload(file: FileDto): Mono<ByteBufferFluxResponse> = fileStorage.doesExist(file)
        .filter { it }
        .switchIfEmptyToNotFound {
            "File with id ${file.requiredId()} is not found"
        }
        .map {
            logger.info("Sending file ${file.name} to a client")
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileStorage.download(file))
        }

    @Operation(
        method = "GET",
        summary = "Download save-agent with current save-cloud version.",
        description = "Download save-agent with current save-cloud version.",
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @ApiResponse(responseCode = "404", description = "File is not found.")
    @RequestMapping(
        path = ["/internal/files/download-save-agent"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        method = [RequestMethod.GET, RequestMethod.POST]
    )
    // FIXME: backend should set version of save-agent here for agent
    fun downloadSaveAgent(): Mono<out Resource> =
            run {
                val executable = "save-agent.kexe"

                downloadFromClasspath(executable) {
                    "Can't find $executable"
                }
            }

    @Operation(
        method = "GET",
        summary = "Download save-cli by version.",
        description = "Download save-cli by version.",
    )
    @Parameter(
        name = "version",
        `in` = ParameterIn.QUERY,
        description = "version of save-cli",
        required = true
    )
    @ApiResponse(responseCode = "200", description = "Returns content of the file.")
    @RequestMapping(
        path = ["/internal/files/download-save-cli"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        method = [RequestMethod.GET, RequestMethod.POST]
    )
    fun downloadSaveCliByVersion(
        @RequestParam version: String,
    ): Mono<out Resource> =
            run {
                val executable = "save-$version-linuxX64.kexe"

                downloadFromClasspath(executable) {
                    "Can't find $executable with the requested version $version"
                }
            }

    /**
     * @param filePartMono a file to be uploaded
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return [Mono] with response
     */
    @PostMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Suppress("UnsafeCallOnNullableType")
    fun upload(
        @RequestPart("file") filePartMono: Mono<FilePart>,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<FileDtoResponse> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.WRITE
    )
        .flatMap { project ->
            filePartMono.flatMap { filePart ->
                val file = fileService.createNew(
                    project = project,
                    name = filePart.filename(),
                )
                val fileDto = file.toDto()
                fileStorage.doesExist(fileDto)
                    .filter { !it }
                    .switchIfEmptyToResponseException(HttpStatus.CONFLICT)
                    .flatMap {
                        fileStorage.upload(fileDto, filePart.content().map { it.asByteBuffer() })
                            .map {
                                fileService.update(file, it)
                            }
                            .thenReturn(fileDto)
                    }
                    .filter { it.sizeBytes > 0 }
                    .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR)
                    .map { ResponseEntity.ok(it) }
            }
        }

    /**
     * @param partMono image to be uploaded
     * @param owner owner name
     * @param type type of avatar
     * @return [Mono] with response
     */
    @Suppress("UnsafeCallOnNullableType")
    @PostMapping(path = ["/api/$v1/image/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") partMono: Mono<FilePart>,
        @RequestParam owner: String,
        @RequestParam type: AvatarType
    ): Mono<StringResponse> = partMono.flatMap { part ->
        val avatarKey = AvatarKey(
            type,
            owner,
        )
        val content = part.content().map { it.asByteBuffer() }
        avatarStorage.upsert(avatarKey, content).map {
            logger.info("Saved $it bytes of $avatarKey")
            avatarKey.getRelativePath()
        }
    }.map { path ->
        when (type) {
            AvatarType.ORGANIZATION -> organizationService.saveAvatar(owner, path)
            AvatarType.USER -> userDetailsService.saveAvatar(owner, path)
        }
        ResponseEntity.status(HttpStatus.OK).body("Image was successfully uploaded")
    }

    /**
     * @param testExecutionDto
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @PostMapping(path = ["/api/$v1/files/get-debug-info"])
    fun getDebugInfo(
        @RequestBody testExecutionDto: TestExecutionDto,
    ): Flux<ByteBuffer> {
        val executionId = getExecutionId(testExecutionDto)
        val testResultLocation = TestResultLocation.from(testExecutionDto)

        return debugInfoStorage.download(DebugInfoStorageKey(executionId, testResultLocation))
            .switchIfEmpty(
                Mono.fromCallable {
                    logger.warn("Additional file for $executionId and $testResultLocation not found")
                }
                    .toFlux()
                    .flatMap {
                        Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"))
                    }
            )
    }

    private fun getExecutionId(testExecutionDto: TestExecutionDto): Long {
        testExecutionDto.executionId?.let { return it }

        val agentContainerId = testExecutionDto.agentContainerId
            .orResponseStatusException(HttpStatus.BAD_REQUEST) {
                "Request body should contain agentContainerId"
            }
        return agentService.getExecutionByContainerId(agentContainerId).requiredId()
    }

    /**
     * @param testExecutionDto
     * @return [Mono] with response
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @PostMapping(path = ["/api/$v1/files/get-execution-info"])
    fun getExecutionInfo(
        @RequestBody testExecutionDto: TestExecutionDto,
    ): Flux<ByteBuffer> {
        logger.debug("Processing getExecutionInfo : $testExecutionDto")
        val executionId = getExecutionId(testExecutionDto)
        return executionInfoStorage.download(executionId)
            .switchIfEmpty(
                Mono.fromCallable {
                    logger.debug("ExecutionInfo for $executionId not found")
                }
                    .toFlux()
                    .flatMap {
                        Flux.error(ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"))
                    }
            )
    }

    /**
     * @param executionId ID of execution that was executed for the test
     * @param testResultDebugInfo additional info that should be stored
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping(value = ["/internal/files/debug-info"])
    fun uploadDebugInfo(
        @RequestParam executionId: Long,
        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> = debugInfoStorage.save(executionId, testResultDebugInfo)

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)
    }
}
