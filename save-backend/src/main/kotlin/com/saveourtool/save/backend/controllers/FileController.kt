package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.storage.MigrationFileStorage
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.Project
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A Spring controller for [FileInfo], [FileKey]
 */
@RestController
@RequestMapping("/api/$v1/files")
@ApiSwaggerSupport
@Tags(
    Tag(name = "files"),
)
class FileController(
    private val fileStorage: MigrationFileStorage,
    private val projectService: ProjectService,
) {
    @Operation(
        method = "GET",
        summary = "Get a list by project coordinates.",
        description = "Get a list by project coordinates.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Returns the list of files.")
    @ApiResponse(responseCode = "404", description = "Not found project by provided values.")
    @GetMapping(path = ["/{organizationName}/{projectName}/list"])
    fun list(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Flux<FileInfo> = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.READ,
    )
        .flatMapMany { project ->
            fileStorage.getFileInfoList(project.toProjectCoordinates())
        }

    @Operation(
        method = "DELETE",
        summary = "Delete a file by project coordinates, name and uploaded time in millis.",
        description = "Delete a file by project coordinates, name and uploaded time in millis.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
        Parameter(name = "name", `in` = ParameterIn.QUERY, description = "name of a file key", required = true),
        Parameter(name = "uploadedMillis", `in` = ParameterIn.QUERY, description = "uploaded mills of a file key", required = true),
    )
    @ApiResponse(responseCode = "200", description = "File deleted successfully.")
    @ApiResponse(responseCode = "404", description = "Not found project or file by provided values.")
    @DeleteMapping(path = ["/{organizationName}/{projectName}/delete"])
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam name: String,
        @RequestParam uploadedMillis: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.DELETE,
    )
        .flatMap { project ->
            fileStorage.delete(FileKey(project.toProjectCoordinates(), name, uploadedMillis))
        }
        .map { deleted ->
            if (deleted) {
                ResponseEntity.ok("File deleted successfully")
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found by uploadedMillis $uploadedMillis in $organizationName/$projectName")
            }
        }

    @Operation(
        method = "GET",
        summary = "Download a file by project coordinates, name and uploaded time in millis.",
        description = "Download a file by project coordinates, name and uploaded time in millis.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
        Parameter(name = "name", `in` = ParameterIn.QUERY, description = "name of a file key", required = true),
        Parameter(name = "uploadedMillis", `in` = ParameterIn.QUERY, description = "uploaded mills of a file key", required = true),
    )
    @ApiResponse(responseCode = "200", description = "The file uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "Not found project or file by provided values.")
    @RequestMapping(
        path = ["/{organizationName}/{projectName}/download"],
        produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE],
        method = [RequestMethod.GET, RequestMethod.POST]
    )
    fun download(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam name: String,
        @RequestParam uploadedMillis: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.READ,
    )
        .flatMap { project ->
            val fileKey = FileKey(
                projectCoordinates = project.toProjectCoordinates(),
                name = name,
                uploadedMillis = uploadedMillis,
            )
            fileStorage.doesExist(fileKey)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "File with key $fileKey is not found"
                }
                .map {
                    log.info("Sending file ${fileKey.name} to a client")
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileStorage.download(fileKey))
                }
        }

    @Operation(
        method = "POST",
        summary = "Upload a file by project coordinates, name and uploaded time in millis.",
        description = "Upload a file by project coordinates, name and uploaded time in millis.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "organization name of a file key", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "project name of a file key", required = true),
        Parameter(name = "file", `in` = ParameterIn.DEFAULT, description = "a file to upload", required = true),
    )
    @ApiResponse(responseCode = "200", description = "The file uploaded successfully.")
    @ApiResponse(responseCode = "404", description = "Not found project or file by provided values.")
    @PostMapping(path = ["/{organizationName}/{projectName}/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @RequestPart("file") file: Mono<FilePart>,
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ) = getProjectAsMonoAndValidatePermission(
        organizationName = organizationName,
        projectName = projectName,
        authentication = authentication,
        permission = Permission.WRITE,
    )
        .flatMap { project ->
            file.flatMap { part ->
                val fileKey = FileKey(
                    project.toProjectCoordinates(),
                    part.filename(),
                    System.currentTimeMillis(),
                )
                fileStorage.doesExist(fileKey)
                    .filter { !it }
                    .switchIfEmptyToResponseException(HttpStatus.CONFLICT)
                    .flatMap {
                        fileStorage.upload(fileKey, part.content().map { it.asByteBuffer() })
                            .map { FileInfo(fileKey, it) }
                    }
                    .filter { it.sizeBytes > 0 }
                    .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR)
                    .map { fileInfo ->
                        ResponseEntity.ok(fileInfo)
                    }
            }
        }

    private fun getProjectAsMonoAndValidatePermission(
        organizationName: String,
        projectName: String,
        authentication: Authentication,
        permission: Permission,
    ): Mono<Project> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectName, organizationName, permission
    )

    companion object {
        private val log = LoggerFactory.getLogger(FileController::class.java)
    }
}
