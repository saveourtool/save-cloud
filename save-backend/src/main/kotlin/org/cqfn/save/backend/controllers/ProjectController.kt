package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.StringResponse
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.LnkUserProjectService
import org.cqfn.save.backend.service.OrganizationService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.AuthenticationDetails
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.domain.Role
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.cqfn.save.permission.Permission
import org.cqfn.save.v1

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Controller for working with projects.
 */
@RestController
@RequestMapping(path = ["/api/$v1/projects"])
class ProjectController(
    private val projectService: ProjectService,
    private val gitService: GitService,
    private val organizationService: OrganizationService,
    private val projectPermissionEvaluator: ProjectPermissionEvaluator,
    private val lnkUserProjectService: LnkUserProjectService,
    private val userRepository: UserRepository,
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
    @PreAuthorize("hasRole('VIEWER')")
    fun getProjectByNameAndOrganizationName(@RequestParam name: String,
                                            @RequestParam organizationName: String,
                                            authentication: Authentication,
    ): Mono<Project> {
        val project = Mono.fromCallable {
            projectService.findByNameAndOrganizationName(name, organizationName)
        }
        return with(projectPermissionEvaluator) {
            project.filterByPermission(authentication, Permission.WRITE, HttpStatus.FORBIDDEN)
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
     * @param project
     * @param authentication
     * @return gitDto
     */
    @PostMapping("/git")
    @Suppress("UnsafeCallOnNullableType")
    fun getRepositoryDtoByProject(@RequestBody project: Project, authentication: Authentication): Mono<GitDto> = Mono.fromCallable {
        with(project) {
            projectService.findWithPermissionByNameAndOrganization(authentication, name, organization, Permission.WRITE)
        }
    }
        .mapNotNull {
            gitService.getRepositoryDtoByProject(project)
        }
        .cast<GitDto>()
        .switchIfEmpty {
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
        }

    /**
     * @param newProjectDto newProjectDto
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    @PostMapping("/save")
    @Suppress("UnsafeCallOnNullableType")
    fun saveProject(@RequestBody newProjectDto: NewProjectDto, authentication: Authentication): ResponseEntity<String> {
        val userId = (authentication.details as AuthenticationDetails).id
        val organization = organizationService.findByName(newProjectDto.organizationName)
        val newProject = newProjectDto.project.apply {
            this.organization = organization!!
        }
        val (projectId, projectStatus) = projectService.getOrSaveProject(
            newProject.apply {
                this.userId = userId
            }
        )
        if (projectStatus == ProjectSaveStatus.EXIST) {
            log.warn("Project with id = $projectId already exists")
            return ResponseEntity.badRequest().body(projectStatus.message)
        }
        log.info("Save new project id = $projectId")
        newProjectDto.gitDto?.let {
            val saveGit = gitService.saveGit(it, projectId)
            log.info("Save new git id = ${saveGit.id}")
        }
        lnkUserProjectService.setRoleByIds(userId, projectId, Role.OWNER)
        return ResponseEntity.ok(projectStatus.message)
    }

    /**
     * @param project
     * @param authentication
     * @return response
     */
    @PostMapping("/update")
    fun updateProject(@RequestBody project: Project, authentication: Authentication): Mono<StringResponse> = projectService.findWithPermissionByNameAndOrganization(
        authentication, project.name, project.organization, Permission.WRITE
    )
        .filter { projectPermissionEvaluator.hasPermission(authentication, project, Permission.WRITE) }
        .map { projectFromDb ->
            // fixme: instead of manually updating fields, a special ProjectUpdateDto could be introduced
            projectFromDb.apply {
                name = project.name
                description = project.description
                url = project.url
                email = project.email
                public = project.public
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
