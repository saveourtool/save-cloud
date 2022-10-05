package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.configs.ApiSwaggerSupport
import com.saveourtool.save.backend.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.LnkUserProjectService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.utils.AuthenticationDetails
import com.saveourtool.save.domain.ProjectSaveStatus
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.filters.ProjectFilters
import com.saveourtool.save.permission.Permission
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
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Controller for working with projects.
 */
@ApiSwaggerSupport
@Tags(
    Tag(name = "projects"),
)
@RestController
@RequestMapping(path = ["/api/$v1/projects"])
@Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
class ProjectController(
    private val projectService: ProjectService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    @GetMapping("/all")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "GET",
        summary = "Get all projects.",
        description = "Get all projects, including deleted and private. Only accessible for super admins",
    )
    @ApiResponse(responseCode = "200", description = "Projects successfully fetched.")
    fun getProjects(): Flux<Project> = projectService.getProjects()

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

    @PostMapping("/not-deleted")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "POST",
        summary = "Get non-deleted projects.",
        description = "Get non-deleted projects, available for current user.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched non-deleted projects.")
    fun getNotDeletedProjectsWithFilters(
        @RequestBody(required = false) projectFilters: ProjectFilters?,
        authentication: Authentication?,
    ): Flux<Project> = projectService.getNotDeletedProjectsWithFilter(projectFilters)
        .toFlux()
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }

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
    ): Mono<Project> {
        val project = Mono.fromCallable {
            projectService.findByNameAndOrganizationName(name, organizationName)
        }
        return with(projectPermissionEvaluator) {
            project.filterByPermission(authentication, Permission.READ, HttpStatus.FORBIDDEN)
        }
    }

    @GetMapping("/get/projects-by-organization")
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get all projects by organization name.",
        description = "Get all projects by organization name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched projects by organization name.")
    fun getProjectsByOrganizationName(
        @RequestParam organizationName: String,
        authentication: Authentication?,
    ): Flux<Project> = projectService.findByOrganizationName(organizationName)
        .filter {
            projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ)
        }

    @GetMapping("/get/not-deleted-projects-by-organization")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "GET",
        summary = "Get non-deleted projects by organization name.",
        description = "Get non-deleted projects by organization name.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched projects by organization name.")
    fun getNonDeletedProjectsByOrganizationName(
        @RequestParam organizationName: String,
        authentication: Authentication?,
    ): Flux<Project> = projectService.getNotDeletedProjectsByOrganizationName(organizationName, authentication)

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
                organizationService.findByName(it.organizationName).toMono(),
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

    @DeleteMapping("/{organizationName}/{projectName}/delete")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("permitAll()")
    @Operation(
        method = "DELETE",
        summary = "Delete a project.",
        description = "Delete a project.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully deleted a project.")
    @ApiResponse(responseCode = "403", description = "Not enough permission for project deletion.")
    @ApiResponse(responseCode = "404", description = "Either could not find such organization or such project in such organization.")
    fun deleteProject(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication
    ): Mono<StringResponse> =
            projectService.findWithPermissionByNameAndOrganization(
                authentication, projectName, organizationName, Permission.DELETE
            )
                .map { projectFromDb ->
                    projectFromDb.apply {
                        status = ProjectStatus.DELETED
                    }
                }
                .map { updatedProject ->
                    projectService.updateProject(updatedProject)
                    ResponseEntity.ok("Successfully deleted project")
                }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectController::class.java)
    }
}
