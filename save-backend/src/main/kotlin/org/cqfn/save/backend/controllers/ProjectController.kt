package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.GitService
import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.Project
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

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

    @GetMapping("/getProject")
    fun geProjectByNameAndOwner(@RequestParam name: String, @RequestParam owner: String) =
        projectService.getProjectByNameAndOwner(name, owner)

    @PostMapping("/executionRequest")
    fun getRequest(@RequestBody project: Project): ExecutionRequest {
        return ExecutionRequest (
            project,
            gitService.getRepositoryByProject(project)
        )
    }
}
