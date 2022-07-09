package com.saveourtool.save.backend.controllers

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.backend.ByteArrayResponse
import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.TestDataFilesystemRepository
import com.saveourtool.save.backend.repository.TimestampBasedFileSystemRepository
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
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
import reactor.core.publisher.Mono

import java.io.FileNotFoundException

import kotlin.io.path.*

/**
 * A Spring controller for file downloading
 */
@RestController
@Suppress("LongParameterList")
class DownloadFilesController(
    private val additionalToolsFileSystemRepository: TimestampBasedFileSystemRepository,
    private val testDataFilesystemRepository: TestDataFilesystemRepository,
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
     * @return a list of files in [additionalToolsFileSystemRepository]
     */
    @GetMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/list"])
    @Suppress("UnsafeCallOnNullableType")
    fun list(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<List<FileInfo>> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.READ
    ).map {
        additionalToolsFileSystemRepository.getFileInfoList(ProjectCoordinates(organizationName, projectName))
    }

    /**
     * @param organizationName
     * @param projectName
     * @param creationTimestamp
     * @param authentication
     * @return [Mono] with response
     */
    @DeleteMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/{creationTimestamp}"])
    @Suppress("UnsafeCallOnNullableType")
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @PathVariable creationTimestamp: String,
        authentication: Authentication,
    ) = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.DELETE
    ).map {
        additionalToolsFileSystemRepository.deleteFileByDirName(ProjectCoordinates(organizationName, projectName), creationTimestamp)
        ResponseEntity.ok("File deleted successfully")
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
    ): Mono<ByteArrayResponse> = Mono.fromCallable {
        logger.info("Sending file ${fileInfo.name} to a client")
        ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(
            additionalToolsFileSystemRepository.getFile(fileInfo, ProjectCoordinates(organizationName, projectName)).inputStream.readAllBytes()
        )
    }
        .doOnError(FileNotFoundException::class.java) {
            logger.warn("File $fileInfo is not found", it)
        }
        .onErrorReturn(
            FileNotFoundException::class.java,
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        )

    /**
     * @param file a file to be uploaded
     * @param returnShortFileInfo whether return FileInfo or ShortFileInfo
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return [Mono] with response
     */
    @PostMapping(path = ["/api/$v1/files/{organizationName}/{projectName}/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Suppress("UnsafeCallOnNullableType")
    fun upload(
        @RequestPart("file") file: Mono<FilePart>,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "true") returnShortFileInfo: Boolean,
        authentication: Authentication,
    ) = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, Permission.WRITE
    ).flatMap {
        additionalToolsFileSystemRepository.saveFile(file, ProjectCoordinates(organizationName, projectName))
    }.map { fileInfo ->
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
     * @param file image to be uploaded
     * @param owner owner name
     * @param type type of avatar
     * @return [Mono] with response
     */
    @Suppress("UnsafeCallOnNullableType")
    @PostMapping(path = ["/api/$v1/image/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadImage(
        @RequestPart("file") file: Mono<FilePart>,
        @RequestParam owner: String,
        @RequestParam type: AvatarType
    ) =
            additionalToolsFileSystemRepository.saveImage(file, owner, type).map { imageInfo ->
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
     * @return [Mono] with response
     * @throws ResponseStatusException if request is invalid or result cannot be returned
     */
    @Suppress("ThrowsCount", "UnsafeCallOnNullableType")
    @PostMapping(path = ["/api/$v1/files/get-debug-info"])
    fun getDebugInfo(
        @RequestBody testExecutionDto: TestExecutionDto,
    ): String {
        val executionId = getExecutionId(testExecutionDto)
        val testResultLocation = TestResultLocation.from(testExecutionDto)
        val debugInfoFile = testDataFilesystemRepository.getLocation(
            executionId,
            testResultLocation
        )
        return if (debugInfoFile.notExists()) {
            logger.warn("File $debugInfoFile not found")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        } else {
            debugInfoFile.readText()
        }
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
    ): String {
        logger.debug("Processing getExecutionInfo : $testExecutionDto")
        val executionId = getExecutionId(testExecutionDto)
        val executionInfoFile = testDataFilesystemRepository.getExecutionInfoFile(executionId).file
        return if (executionInfoFile.exists()) {
            val text = executionInfoFile.readText()
            logger.debug("Sending $executionInfoFile : $text")
            text
        } else {
            logger.debug("File ${executionInfoFile.absolutePath} not found")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "File not found")
        }
    }

    /**
     * @param agentContainerId agent that has executed the test
     * @param testResultDebugInfo additional info that should be stored
     * @return [Mono] with response
     */
    @PostMapping(value = ["/internal/files/debug-info"])
    @Suppress("UnsafeCallOnNullableType")
    fun uploadDebugInfo(@RequestParam("agentId") agentContainerId: String,
                        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ) {
        val executionId = agentRepository.findByContainerId(agentContainerId)!!.execution.id!!
        testDataFilesystemRepository.save(executionId, testResultDebugInfo)
    }
}
