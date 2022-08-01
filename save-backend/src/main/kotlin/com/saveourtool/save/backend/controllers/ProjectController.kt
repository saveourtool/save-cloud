package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.StringResponse
import com.saveourtool.save.backend.security.ProjectPermissionEvaluator
import com.saveourtool.save.backend.service.LnkUserProjectService
import com.saveourtool.save.backend.service.OrganizationService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.ProjectSaveStatus
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.v1

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

/**
 * Controller for working with projects.
 */
@RestController
@RequestMapping(path = ["/api/$v1/projects"])
class ProjectController(
    private val projectService: ProjectService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val lnkUserProjectService: LnkUserProjectService,
) {
    /**
     * Get all projects, including deleted and private. Only accessible for admins.
     *
     * @return a list of projects
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    fun getProjects() = projectService.getProjects()

    /**
     * Get all projects, accessible for the current user.
     * Note: `@PostFilter` is not yet supported for webflux: https://github.com/spring-projects/spring-security/issues/5249
     *
     * @param authentication [Authentication] describing an authenticated request
     * @return flux of projects
     */
    @GetMapping("/")
    fun getProjects(authentication: Authentication): Flux<Project> = projectService.getProjects()
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ) }

    /**
     * Get all projects without status.
     *
     * @param authentication
     * @return a list of projects
     */
    @GetMapping("/not-deleted")
    fun getNotDeletedProjects(authentication: Authentication?) = projectService.getNotDeletedProjects()
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ) }

    /**
     * @param name
     * @param organizationName
     * @param authentication
     * @return project by name and organization name
     */
    @GetMapping("/get/organization-name")
    @PreAuthorize("permitAll()")
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

    /**
     * @param organizationName
     * @param authentication
     * @return project by name and organization name
     */
    @GetMapping("/get/projects-by-organization")
    @PreAuthorize("permitAll()")
    fun getProjectsByOrganizationName(@RequestParam organizationName: String,
                                      authentication: Authentication?,
    ): Flux<Project> = projectService.findByOrganizationName(organizationName)
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ) }

    /**
     * @param organizationName
     * @param authentication
     * @return non deleted project by name and organization name
     */
    @GetMapping("/get/not-deleted-projects-by-organization")
    @PreAuthorize("permitAll()")
    fun getNonDeletedProjectsByOrganizationName(@RequestParam organizationName: String,
                                                authentication: Authentication?,
    ): Flux<Project> = projectService.findByOrganizationName(organizationName)
        .filter { it.status != ProjectStatus.DELETED }
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, Permission.READ) }

    /**
     * @param projectCreationRequest [ProjectDto]
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    @Suppress("UnsafeCallOnNullableType", "TOO_LONG_FUNCTION", "TYPE_ALIAS")
    fun saveProject(
        @RequestBody projectCreationRequest: ProjectDto,
        authentication: Authentication,
    ): Mono<ResponseEntity<String>> = Mono.just(projectCreationRequest)
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

    /**
     * @param projectDto
     * @param authentication
     * @return response
     */
    @PostMapping("/update")
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
            val (_, projectStatus) = projectService.getOrSaveProject(updatedProject)
            ResponseEntity.ok(projectStatus.message)
        }

    /**
     * @param organizationName
     * @param projectName
     * @param authentication
     * @return response
     */
    @DeleteMapping("/{organizationName}/{projectName}/delete")
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
                    val (_, projectStatus) = projectService.getOrSaveProject(updatedProject)
                    ResponseEntity.ok(projectStatus.message)
                }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectController::class.java)
    }
}
