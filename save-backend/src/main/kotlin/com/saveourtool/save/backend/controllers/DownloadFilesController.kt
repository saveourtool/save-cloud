package com.saveourtool.save.backend.controllers

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.backend.storage.*
import com.saveourtool.save.backend.storage.AvatarKey
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.domain.*
import com.saveourtool.save.from
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.AvatarType
import com.saveourtool.save.v1

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

import java.io.FileNotFoundException
import java.nio.ByteBuffer

/**
 * A Spring controller for file downloading
 */
@RestController
@Suppress("LongParameterList")
class DownloadFilesController(
    private val fileStorage: FileStorage,
    private val avatarStorage: AvatarStorage,
    private val debugInfoStorage: DebugInfoStorage,
    private val executionInfoStorage: ExecutionInfoStorage,
    private val agentRepository: AgentRepository,
    private val organizationService: OrganizationService,
    private val userDetailsService: UserDetailsService,
    private val projectService: ProjectService,
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
    ): Flux<FileInfo> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.READ
    )
        .toFlux()
        .flatMap {
            fileStorage.getFileInfoList(ProjectCoordinates(organizationName, projectName))
        }

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @param fileInfo
     * @return [Mono] with response
     */
    @DeleteMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/{creationTimestamp}"])
    @Suppress("UnsafeCallOnNullableType")
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @PathVariable creationTimestamp: String,
        @RequestBody fileInfo: FileInfo,
        authentication: Authentication,
    ) = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.DELETE
    ).flatMap {
        fileStorage.deleteByUploadedMillis(ProjectCoordinates(organizationName, projectName), creationTimestamp.toLong())
    }.map { deleted ->
        if (deleted) {
            ResponseEntity.ok("File deleted successfully")
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("File not found by creationTimestamp $creationTimestamp in $organizationName/$projectName")
        }
    }

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @param organizationName
     * @param projectName
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(
        @RequestBody fileInfo: FileInfo,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<ByteBufferFluxResponse> = downloadByFileKey(fileInfo.toFileKey(), organizationName, projectName)

    /**
     * @param fileKey a key [FileKey] of requested file
     * @param organizationName
     * @param projectName
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/internal/files/{organizationName}/{projectName}/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadByFileKey(
        @RequestBody fileKey: FileKey,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<ByteBufferFluxResponse> = download(ProjectCoordinates(organizationName, projectName), fileKey)

    /**
     * @param projectCoordinates
     * @param fileKey
     */
    @PostMapping(path = ["/internal/files/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun download(
        @RequestPart("projectCoordinates") projectCoordinates: ProjectCoordinates,
        @RequestPart("fileKey") fileKey: FileKey
    ): Mono<ByteBufferFluxResponse> = Mono.fromCallable {
        logger.info("Sending file ${fileKey.name} to a client")
        val content = fileStorage.download(projectCoordinates, fileKey)
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content)
    }
        .doOnError(FileNotFoundException::class.java) {
            logger.warn("File with key $fileKey is not found", it)
        }
        .onErrorReturn(
            FileNotFoundException::class.java,
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        )

    /**
     * @param fileMono a file to be uploaded
     * @param returnShortFileInfo whether return FileInfo or ShortFileInfo
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return [Mono] with response
     */
    @PostMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Suppress("UnsafeCallOnNullableType")
    fun upload(
        @RequestPart("file") fileMono: Mono<FilePart>,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "true") returnShortFileInfo: Boolean,
        authentication: Authentication,
    ) = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.WRITE
    )
        .flatMap {
            fileStorage.uploadFilePart(fileMono, ProjectCoordinates(organizationName, projectName))
        }
        .map { fileInfo ->
            ResponseEntity.status(
                if (fileInfo.sizeBytes > 0) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
            )
                .body(
                    if (returnShortFileInfo) {
                        fileInfo.toShortFileInfo()
                    } else {
                        fileInfo
                    }
                )
        }
        .onErrorReturn(
            FileAlreadyExistsException::class.java,
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        )

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
    ) = partMono.flatMap { part ->
        val avatarKey = AvatarKey(
            type,
            owner,
            part.filename()
        )
        val content = part.content().map { it.asByteBuffer() }
        avatarStorage.upload(avatarKey, content).map {
            logger.info("Saved $it bytes of $avatarKey")
            ImageInfo(avatarKey.getRelativePath())
        }
    }.map { imageInfo ->
        imageInfo.path?.let {
            when (type) {
                AvatarType.ORGANIZATION -> organizationService.saveAvatar(owner, it)
                AvatarType.USER -> userDetailsService.saveAvatar(owner, it)
            }
        }
        ResponseEntity.status(
            imageInfo.path?.let {
                HttpStatus.OK
            } ?: HttpStatus.INTERNAL_SERVER_ERROR
        )
            .body(imageInfo)
    }
        .onErrorReturn(
            FileAlreadyExistsException::class.java,
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        )

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

        return debugInfoStorage.download(Pair(executionId, testResultLocation))
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
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body should contain agentContainerId")
        val execution = agentRepository.findByContainerId(agentContainerId)?.execution
        return execution?.id
            ?: throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Execution for agent $agentContainerId not found"
            )
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
     * @param agentContainerId agent that has executed the test
     * @param testResultDebugInfo additional info that should be stored
     * @return [Mono] with count of uploaded bytes
     */
    @PostMapping(value = ["/internal/files/debug-info"])
    @Suppress("UnsafeCallOnNullableType")
    fun uploadDebugInfo(@RequestParam("agentId") agentContainerId: String,
                        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> {
        val executionId = agentRepository.findByContainerId(agentContainerId)!!.execution.id!!
        return debugInfoStorage.save(executionId, testResultDebugInfo)
    }
}
