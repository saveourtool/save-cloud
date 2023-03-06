package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.LnkProjectGithubService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.demo.DemoCreationRequest
import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.Project
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Controller that allows adding tools to save-demo
 */
@RestController
@RequestMapping("/api/$v1/demo")
class DemoManagerController(
    private val projectService: ProjectService,
    private val lnkProjectGithubService: LnkProjectGithubService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    configProperties: ConfigProperties,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientDemo = WebClient.builder()
        .baseUrl(configProperties.demoUrl)
        .applyAll(customizers)
        .build()

    @PostMapping("/{organizationName}/{projectName}/add")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "POST",
        summary = "Add demo for a tool.",
        description = "Add demo for a tool.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully added demo.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization.")
    @ApiResponse(responseCode = "409", description = "Invalid demo creation request.")
    fun addDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestBody demoCreationRequest: DemoCreationRequest,
        authentication: Authentication,
    ): Mono<StringResponse> = projectService.projectByCoordinatesOrNotFound(projectName, organizationName) {
        "Could not find project $projectName in organization $organizationName."
    }
        .requireOrSwitchToResponseException({ projectPermissionEvaluator.hasPermission(authentication, this, Permission.DELETE) }, HttpStatus.FORBIDDEN) {
            "Not enough permission for accessing given project."
        }
        .asyncEffect { project ->
            blockingToMono {
                demoCreationRequest.demoDto.githubProjectCoordinates?.let { githubCoordinates ->
                    lnkProjectGithubService.saveIfNotPresent(
                        project,
                        githubCoordinates.organizationName,
                        githubCoordinates.projectName,
                    )
                }
            }
        }
        .flatMap {
            webClientDemo.post()
                .uri("/demo/internal/manager/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(demoCreationRequest.demoDto)
                .retrieve()
                .onStatus({ it == HttpStatus.CONFLICT }) {
                    Mono.error(
                        ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Demo creation request is invalid: fill project coordinates, run command and file name.",
                        )
                    )
                }
                .toBodilessEntity()
        }
        .flatMap {
            webClientDemo.post()
                .uri("/demo/internal/files/${demoCreationRequest.demoDto.projectCoordinates}/upload?version=manual")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(demoCreationRequest.manuallyUploadedFileDtos)
                .retrieve()
                .toBodilessEntity()
        }
        .map {
            StringResponse.ok("Successfully signed up ${demoCreationRequest.demoDto.projectCoordinates} demo.")
        }

    @PostMapping("/{organizationName}/{projectName}/upload-file", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version to attach the file to, manual by default", required = false),
        Parameter(name = "file", `in` = ParameterIn.DEFAULT, description = "a file to upload", required = true),
    )
    @Operation(
        method = "POST",
        summary = "Attach file to demo.",
        description = "Attach file to demo.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully uploaded file to demo.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization.")
    fun uploadFile(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
        @RequestPart file: FilePart,
        authentication: Authentication,
    ): Mono<FileDto> = forwardRequestCheckingPermission(Permission.DELETE, organizationName, projectName, authentication) {
        webClientDemo.post()
            .uri("/demo/internal/files/$organizationName/$projectName/upload?version=$version")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData("file", file))
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .bodyToMono()
    }

    @GetMapping("/{organizationName}/{projectName}/list-file")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version of files, manual by default", required = false),
    )
    @Operation(
        method = "GET",
        summary = "Get list of files.",
        description = "Get list of files attached to requested version of saveourtool project demo.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched list of files.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization.")
    fun listFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
        authentication: Authentication,
    ): Flux<FileDto> = projectService.projectByCoordinatesOrNotFound(projectName, organizationName) {
        "Could not find project $projectName in organization $organizationName."
    }
        .requireOrSwitchToResponseException({ projectPermissionEvaluator.hasPermission(authentication, this, Permission.DELETE) }, HttpStatus.FORBIDDEN) {
            "Not enough permission for accessing given project."
        }
        .flatMapMany {
            webClientDemo.get()
                .uri("/demo/internal/files/$organizationName/$projectName/list?version=$version")
                .retrieve()
                .defaultNotFoundProcessing(organizationName, projectName)
                .bodyToFlux()
        }

    @DeleteMapping("/{organizationName}/{projectName}/delete-file")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "version of file, manual by default", required = false),
        Parameter(name = "version", `in` = ParameterIn.QUERY, description = "name of file to be deleted", required = true),
    )
    @Operation(
        method = "DELETE",
        summary = "Delete a file.",
        description = "Delete a file.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted file.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization.")
    fun deleteFile(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
        @RequestParam fileName: String,
        authentication: Authentication,
    ): Mono<Boolean> = forwardRequestCheckingPermission(Permission.DELETE, organizationName, projectName, authentication) {
        webClientDemo.delete()
            .uri("/demo/internal/files/$organizationName/$projectName/delete?version=$version&fileName=$fileName")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .bodyToMono()
    }

    @GetMapping("/{organizationName}/{projectName}/status")
    @RequiresAuthorizationSourceHeader
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "GET",
        summary = "Get demo status.",
        description = "Get demo status.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched demo status.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization or demo for it.")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<DemoStatus> = forwardRequestCheckingPermission(Permission.READ, organizationName, projectName, authentication) {
        webClientDemo.get()
            .uri("/demo/api/manager/$organizationName/$projectName/status")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .bodyToMono<DemoStatus>()
            .defaultIfEmpty(DemoStatus.NOT_CREATED)
    }

    @GetMapping("/{organizationName}/{projectName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "GET",
        summary = "Get demo info.",
        description = "Get demo info.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched demo status.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project in organization or demo for it.")
    fun getDemoInfo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<DemoInfo> = forwardRequestCheckingPermission(Permission.READ, organizationName, projectName, authentication) {
        webClientDemo.get()
            .uri("/demo/api/manager/$organizationName/$projectName")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .bodyToMono<DemoInfo>()
            .defaultIfEmpty(
                DemoInfo(
                    DemoDto.emptyForProject(organizationName, projectName),
                    DemoStatus.NOT_CREATED,
                )
            )
    }

    @PostMapping("/{organizationName}/{projectName}/delete")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "POST",
        summary = "Delete demo.",
        description = "Delete demo by saveourtool organization and project names.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted demo.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for demo deletion.")
    @ApiResponse(responseCode = "404", description = "Could not find saveourtool project or demo of a project.")
    fun deleteDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = forwardRequestCheckingPermission(Permission.DELETE, organizationName, projectName, authentication) { project ->
        webClientDemo.post()
            .uri("/demo/internal/manager/${project.toProjectCoordinates()}/delete")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .toEntity()
    }

    @PostMapping("/{organizationName}/{projectName}/start")
    @RequiresAuthorizationSourceHeader
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "POST",
        summary = "Start demo container.",
        description = "Start demo container if possible, do nothing otherwise.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully started demo.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for demo management.")
    @ApiResponse(responseCode = "404", description = "Could not find saveourtool project or demo of a project.")
    fun startDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = forwardRequestCheckingPermission(Permission.WRITE, organizationName, projectName, authentication) { project ->
        webClientDemo.post()
            .uri("/demo/internal/manager/${project.toProjectCoordinates()}/start")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .toEntity()
    }

    @PostMapping("/{organizationName}/{projectName}/stop")
    @RequiresAuthorizationSourceHeader
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of saveourtool organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of saveourtool project", required = true),
    )
    @Operation(
        method = "POST",
        summary = "Stop demo.",
        description = "Delete demo container if possible, do nothing otherwise.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully stopped demo.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for demo management.")
    @ApiResponse(responseCode = "404", description = "Could not find saveourtool project or demo of a project.")
    fun stopDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = forwardRequestCheckingPermission(Permission.WRITE, organizationName, projectName, authentication) { project ->
        webClientDemo.post()
            .uri("/demo/internal/manager/${project.toProjectCoordinates()}/stop")
            .retrieve()
            .defaultNotFoundProcessing(organizationName, projectName)
            .toEntity()
    }

    private inline fun <reified T : Any> forwardRequestCheckingPermission(
        requiredPermission: Permission,
        organizationName: String,
        projectName: String,
        authentication: Authentication,
        crossinline requestBuilder: (Project) -> Mono<T>,
    ): Mono<T> = projectService.projectByCoordinatesOrNotFound(projectName, organizationName) {
        "Could not find $organizationName/$projectName."
    }
        .requireOrSwitchToForbidden({ projectPermissionEvaluator.hasPermission(authentication, this, requiredPermission) }) {
            "Not enough permission for accessing $organizationName/$projectName."
        }
        .flatMap { project ->
            requestBuilder(project)
        }

    private fun WebClient.ResponseSpec.defaultNotFoundProcessing(
        organizationName: String,
        projectName: String,
    ) = onStatus({ it == HttpStatus.NOT_FOUND }) {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Could not find demo for $organizationName/$projectName."))
    }
}
