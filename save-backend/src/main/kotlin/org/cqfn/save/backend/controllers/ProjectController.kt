package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for working with projects.
 */
@RestController
class ProjectController {
    @Autowired
    private lateinit var projectService: ProjectService

    @Autowired
    private lateinit var gitService: GitService

    /**
     * Get all projects.
     */
    @GetMapping("/projects")
    fun getProjects() = projectService.getProjects()

    /**
     * @param name name of project
     * @param owner owner of project
     * @return
     */
    @GetMapping("/getProject")
    fun geProjectByNameAndOwner(@RequestParam name: String, @RequestParam owner: String) =
            projectService.getProjectByNameAndOwner(name, owner)?.let {
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
}
