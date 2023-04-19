package com.saveourtool.save.backend.controllers

import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.LnkUserProjectService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.domain.ProjectSaveStatus
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.filters.ProjectFilter
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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.*

/**
 * Controller for working with projects.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "projects"),
)
@RestController
@RequestMapping(path = ["/api/$v1/projects"])
class ProjectController(
    private val projectService: ProjectService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    @GetMapping("/")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all available projects.",
        description = "Get all projects, available for current user.",
    )
    @ApiResponse(responseCode = "200", description = "Projects successfully fetched.")
    fun getProjects(
        authentication: Authentication,
    ): Flux<Project> = projectService.getProjects()
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }

    @PostMapping("/by-filters")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get projects matching filters",
        description = "Get filtered projects available for the current user.",
    )
    @Parameters(
        Parameter(name = "projectFilter", `in` = ParameterIn.DEFAULT, description = "project filters", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched projects.")
    fun getFilteredProjects(
        @RequestBody projectFilter: ProjectFilter,
        authentication: Authentication?,
    ): Flux<ProjectDto> =
            blockingToFlux { projectService.getFiltered(projectFilter) }
                .filter {
                    projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
                }
                .map { it.toDto() }

    @GetMapping("/get/organization-name")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get project by name and organization name.",
        description = "Get project by name and organization name.",
    )
    @Parameters(
        Parameter(name = "name", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched project by name and organization name.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for accessing given project.")
    @ApiResponse(responseCode = "404", description = "Could not find project with such name and organization name.")
    fun getProjectByNameAndOrganizationName(
        @RequestParam name: String,
        @RequestParam organizationName: String,
        authentication: Authentication,
    ): Mono<ProjectDto> {
        val project = Mono.fromCallable {
            projectService.findByNameAndOrganizationNameAndCreatedStatus(name, organizationName)
        }
        return with(projectPermissionEvaluator) {
            project.filterByPermission(authentication, Permission.READ, HttpStatus.FORBIDDEN)
        }.map { it.toDto() }
    }

    @PostMapping("/save")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Create a new project.",
        description = "Create a new project.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully created a new project.")
    @ApiResponse(responseCode = "404", description = "Could not find organization with such name.")
    @ApiResponse(responseCode = "409", description = "Either invalid data, or project with such name is already created.")
    @Suppress("TOO_LONG_FUNCTION")
    fun saveProject(
        @RequestBody projectCreationRequest: ProjectDto,
        authentication: Authentication,
    ): Mono<StringResponse> = Mono.just(projectCreationRequest)
        .flatMap {
            Mono.zip(
                projectCreationRequest.toMono(),
                organizationService.findByNameAndCreatedStatus(it.organizationName).toMono(),
            )
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Couldn't find organization with name ${projectCreationRequest.organizationName}",
            ))
        }
        .filter { (projectDto, _) ->
            projectDto.validate()
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(
                HttpStatus.CONFLICT,
                "Invalid input data: check url and naming validity",
            ))
        }
        .map { (projectDto, organization) ->
            projectService.getOrSaveProject(projectDto.toProject(organization))
        }
        .filter { (_, status) ->
            status == ProjectSaveStatus.NEW
        }
        .switchIfEmpty {
            Mono.error(ResponseStatusException(
                HttpStatus.CONFLICT,
                "Project with name ${projectCreationRequest.name} is already present in organization ${projectCreationRequest.organizationName}",
            ))
        }
        .map { (projectId, status) ->
            lnkUserProjectService.setRoleByIds((authentication.details as AuthenticationDetails).id, projectId, Role.OWNER)
            ResponseEntity.ok(status.message)
        }

    @PostMapping("/update")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Update an existing project.",
        description = "Update an existing project.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully updated a project.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for managing project settings.")
    @ApiResponse(responseCode = "404", description = "Either project or organization could not be found.")
    fun updateProject(
        @RequestBody projectDto: ProjectDto,
        authentication: Authentication,
    ): Mono<StringResponse> = projectService.findWithPermissionByNameAndOrganization(
        authentication, projectDto.name, projectDto.organizationName, Permission.WRITE
    )
        .map { projectFromDb ->
            // fixme: instead of manually updating fields, a special ProjectUpdateDto could be introduced
            projectFromDb.apply {
                name = projectDto.name
                description = projectDto.description
                url = projectDto.url
                email = projectDto.email
                public = projectDto.isPublic
            }
        }
        .map { updatedProject ->
            projectService.updateProject(updatedProject)
            ResponseEntity.ok("Project was successfully updated")
        }

    @PostMapping("/{organizationName}/{projectName}/change-status")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Change status of existing project.",
        description = "Change status of existing project by its name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
        Parameter(name = "status", `in` = ParameterIn.QUERY, description = "type of status being set", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully change status of a project.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for this action on project.")
    @ApiResponse(responseCode = "404", description = "Either could not find such organization or such project in such organization.")
    fun changeProjectStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam status: ProjectStatus,
        authentication: Authentication
    ): Mono<StringResponse> = blockingToMono {
        projectService.findByNameAndOrganizationNameAndStatusIn(projectName, organizationName, EnumSet.allOf(ProjectStatus::class.java))
    }
        .switchIfEmptyToNotFound {
            "Could not find an organization with name $organizationName or project $projectName in organization $organizationName."
        }
        .filter {
            it.status != status
        }
        .switchIfEmptyToResponseException(HttpStatus.BAD_REQUEST) {
            "Invalid new status of the organization $organizationName"
        }
        .filter {
            projectPermissionEvaluator.hasPermissionToChangeStatus(authentication, it, status)
        }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "Not enough permission for this action with organization $organizationName."
        }
        .map { project ->
            when (status) {
                ProjectStatus.BANNED -> {
                    projectService.changeProjectStatus(project, ProjectStatus.BANNED)
                    ResponseEntity.ok("Successfully banned the project")
                }
                ProjectStatus.DELETED -> {
                    projectService.changeProjectStatus(project, ProjectStatus.DELETED)
                    ResponseEntity.ok("Successfully deleted the project")
                }
                ProjectStatus.CREATED -> {
                    projectService.changeProjectStatus(project, ProjectStatus.CREATED)
                    ResponseEntity.ok("Successfully recovered the project")
                }
            }
        }

    @PostMapping("/problem/save")
    @Operation(
        method = "POST",
        summary = "Save project problem.",
        description = "Save project problem.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved project problem")
    @PreAuthorize("permitAll()")
    fun save(
        @RequestBody projectProblemDto: ProjectProblemDto,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        projectService.saveProjectProblem(projectProblemDto, authentication)
    }.map {
        ResponseEntity.ok("Project problem was successfully saved")
    }

    @GetMapping("/problem/all")
    @Operation(
        method = "GET",
        summary = "Get all project problems.",
        description = "Get all project problems.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched all project problems")
    @Suppress("TYPE_ALIAS")
    fun getAllProjectProblems(
        @RequestParam projectName: String,
        @RequestParam organizationName: String,
    ): Mono<List<ProjectProblemDto>> = blockingToMono {
        projectService.getAllProblemsByProjectNameAndProjectOrganizationName(projectName, organizationName).map(
            ProjectProblem::toDto)
    }

    @GetMapping("/problem/get/by-id")
    @Operation(
        method = "GET",
        summary = "Get project problem by id.",
        description = "Get project problem by id.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched project problem")
    fun getProjectProblemById(
        @RequestParam id: Long,
    ): Mono<ProjectProblemDto> = blockingToMono {
        projectService.getProjectProblemById(id).toDto()
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectController::class.java)
    }
}
