package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ProjectService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for working with projects.
 */
@RestController
class ProjectController(private val projectService: ProjectService) {
    /**
     * Get all projects.
     */
    @GetMapping("/projects")
    fun getProjects() = projectService.getProjects()
}
