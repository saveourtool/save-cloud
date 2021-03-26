package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.service.ProjectService
import org.cqfn.save.entities.Project
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ProjectController(private val projectService: ProjectService) {
    @GetMapping("/projects")
    fun getProjects() = projectService.getProjects()
}
