package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.*
import com.saveourtool.save.backend.service.AgentService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.storage.*
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.TestExecution
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

import java.nio.ByteBuffer

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
    private val avatarStorage: MigrationAvatarStorage,
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
) {
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
     * @param testExecutionId [com.saveourtool.save.entities.TestExecution.id]
     * @return [Mono] with content of DebugInfo
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @GetMapping(path = ["/api/$v1/files/get-debug-info"])
    fun getDebugInfo(
        @RequestParam testExecutionId: Long,
    ): Flux<ByteBuffer> = debugInfoStorage.download(testExecutionId)
        .switchIfEmptyToNotFound {
            logger.warn("Additional file for ${TestExecution::class.simpleName} with id $testExecutionId not found")
            "File not found"
        }

    /**
     * @param executionId [com.saveourtool.save.entities.Execution.id]
     * @return [Mono] with response
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @GetMapping(path = ["/api/$v1/files/get-execution-info"])
    fun getExecutionInfo(
        @RequestParam executionId: Long,
    ): Flux<ByteBuffer> = executionInfoStorage.download(executionId)
        .switchIfEmptyToNotFound {
            logger.debug("ExecutionInfo for $executionId not found")
            "File not found"
        }

    companion object {
        private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)
    }
}
