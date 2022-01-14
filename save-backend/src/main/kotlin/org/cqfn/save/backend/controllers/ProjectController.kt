package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.security.ProjectPermissionEvaluator
import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.username
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PostFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.server.EntityResponse
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Controller for working with projects.
 */
@RestController
@RequestMapping("/api/projects")
class ProjectController(private val projectService: ProjectService,
                        private val gitService: GitService,
                        private val projectPermissionEvaluator: ProjectPermissionEvaluator,
) {
    /**
     * Get all projects, including deleted and private. Only accessible for admins.
     *
     * @return a list of projects
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    fun getProjects() = projectService.getProjects()

    /**
     * Get all projects, accessible for the current user.
     * Note: `@PostFilter` is not yet supported for webflux: https://github.com/spring-projects/spring-security/issues/5249
     *
     * @return flux of projects
     */
    @GetMapping("/")
    fun getProjects(authentication: Authentication): Flux<Project> = projectService.getProjects()
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, "read") }

    /**
     * Get all projects without status.
     *
     * @return a list of projects
     */
    @GetMapping("/not-deleted")
    fun getNotDeletedProjects(authentication: Authentication) = projectService.getNotDeletedProjects()
        .filter { projectPermissionEvaluator.hasPermission(authentication, it, "read") }

    /**
     * 200 - if user can access the project
     * 403 - if project is public, but user can't access it
     * 404 - if project is not found or private and user can't access it
     * FixMe: requires 'write' permission, because now we rely on this endpoint to load `ProjectView`
     *
     * @param name name of project
     * @param owner owner of project
     * @return project by name and owner
     */
    @GetMapping("/get")
    @PreAuthorize("hasRole('VIEWER')")
    fun getProjectByNameAndOwner(@RequestParam name: String,
                                 @RequestParam owner: String,
                                 authentication: Authentication,
    ): Mono<Project> {
        return Mono.fromCallable {
            projectService.findByNameAndOwner(name, owner)
        }
            .map {
                // if value is null, then Mono is empty and this lambda won't be called
                it!! to projectPermissionEvaluator.hasPermission(authentication, it, "write")
            }
            .filter { (project, hasWriteAccess) -> project.public || hasWriteAccess }
            .map { (project, hasWriteAccess) ->
                if (hasWriteAccess) {
                    project
                } else {
                    // project is public, but current user lacks permissions
                    throw ResponseStatusException(HttpStatus.FORBIDDEN)
                }
            }
            .switchIfEmpty {
                // if project either is not found or shouldn't be visible for current user
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
            }
    }

    /**
     * @param project
     * @return gitDto
     */
    @PostMapping("/git")
    @PreAuthorize("@projectPermissionEvaluator.hasPermission(authentication, project, 'write')")
    fun getRepositoryDtoByProject(@RequestBody project: Project): Mono<GitDto> =
        Mono.fromCallable {
            gitService.getRepositoryDtoByProject(project)
        }
            .mapNotNull { it!! }
            .switchIfEmpty {
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND))
            }

    /**
     * @param newProjectDto newProjectDto
     * @param authentication an [Authentication] representing an authenticated request
     * @return response
     */
    // todo: change it to use userId from Authentication
    @PostMapping("/save")
    fun saveProject(@RequestBody newProjectDto: NewProjectDto, authentication: Authentication): ResponseEntity<String> {
//        val userId = (authentication.details as Map<String, Any>)["id"] as Long
        val (projectId, projectStatus) = projectService.saveProject(
            newProjectDto.project,
            authentication.username()
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
        return ResponseEntity.ok(projectStatus.message)
    }

    /**
     * @param project
     * @return response
     */
    @PostMapping("/update")
    @PreAuthorize("@projectPermissionEvaluator.hasPermission(authentication, project, 'write')")
    fun updateProject(@RequestBody project: Project): ResponseEntity<String> {
        val (_, projectStatus) = projectService.saveProject(project, null)
        return ResponseEntity.ok(projectStatus.message)
    }

    companion object {
        @JvmStatic
        private val log = LoggerFactory.getLogger(ProjectController::class.java)
    }
}
