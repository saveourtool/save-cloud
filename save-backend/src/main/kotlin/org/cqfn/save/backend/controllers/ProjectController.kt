package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.backend.utils.toUser
import org.cqfn.save.domain.ProjectSaveStatus
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.NewProjectDto
import org.cqfn.save.entities.Project
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import java.security.Principal

/**
 * Controller for working with projects.
 */
@RestController
@RequestMapping("/api")
class ProjectController {
    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var gitService: GitService

    /**
     * Get all projects.
     *
     * @return a list of projects
     */
    @GetMapping("/projects")
    fun getProjects() = projectService.getProjects()

    /**
     * Get all projects without status.
     *
     * @return a list of projects
     */
    @GetMapping("/projects/not-deleted")
    fun getNotDeletedProjects() = projectService.getNotDeletedProjects()

    /**
     * @param name name of project
     * @param owner owner of project
     * @return project by name and owner
     */
    @GetMapping("/getProject")
    fun getProjectByNameAndOwner(@RequestParam name: String, @RequestParam owner: String) =
            projectService.findByNameAndOwner(name, owner)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param project
     * @return gitDto
     */
    @PostMapping("/getGit")
    fun getRepositoryDtoByProject(@RequestBody project: Project): ResponseEntity<GitDto> =
            gitService.getRepositoryDtoByProject(project)?.let {
                ResponseEntity.status(HttpStatus.OK).body(it)
            } ?: ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    /**
     * @param newProjectDto newProjectDto
     * @param principal a Principal of an authenticated request
     * @return response
     */
    @PostMapping("/saveProject")
    fun saveProject(@RequestBody newProjectDto: NewProjectDto, principal: Principal): ResponseEntity<String> {
        val (projectId, projectStatus) = projectService.saveProject(newProjectDto.project, principal.toUser().name)
        if (projectStatus == ProjectSaveStatus.EXIST) {
            log.warn("Project with id = $projectId already exists")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(projectStatus.message)
        }
        log.info("Save new project id = $projectId")
        newProjectDto.gitDto?.let {
            val saveGit = gitService.saveGit(it, projectId)
            log.info("Save new git id = ${saveGit.id}")
        }
        return ResponseEntity.status(HttpStatus.OK).body(projectStatus.message)
    }

    /**
     * @param project
     * @return response
     */
    @PostMapping("/updateProject")
    fun updateProject(@RequestBody project: Project): ResponseEntity<String> {
        val (_, projectStatus) = projectService.saveProject(project, null)
        return ResponseEntity.status(HttpStatus.OK).body(projectStatus.message)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProjectController::class.java)
    }
}
