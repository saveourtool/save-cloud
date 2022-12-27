package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.service.LnkProjectGithubService
import com.saveourtool.save.backend.service.ProjectService
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.NewDemoToolRequest
import com.saveourtool.save.entities.ProjectStatus
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Controller that allows adding tools to save-demo
 */
@RestController
@RequestMapping("/api/$v1/demo")
class DemoManagerController(
    private val projectService: ProjectService,
    private val lnkProjectGithubService: LnkProjectGithubService,
    configProperties: ConfigProperties,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientDemo = WebClient.builder()
        .baseUrl(configProperties.demoUrl)
        .applyAll(customizers)
        .build()

    @PostMapping("/add")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Add demo for a tool.",
        description = "Add demo for a tool.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully added demo.")
    fun addDemo(
        @RequestBody organizationName: String,
        @RequestBody projectName: String,
        @RequestBody demoToolRequest: NewDemoToolRequest,
        authentication: Authentication,
    ): Mono<Unit> = demoToolRequest.toMono()
        .flatMap {
            webClientDemo.post()
                .uri("/demo/internal/add-tool")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(it)
                .retrieve()
                .toBodilessEntity()
        }
        .flatMap {
            projectService.findByNameAndOrganizationNameAndStatusIn(projectName, organizationName, setOf(ProjectStatus.CREATED)).toMono()
        }
        .map { project ->
            lnkProjectGithubService.saveIfNotPresent(project, demoToolRequest.ownerName, demoToolRequest.repoName)
            Unit
        }

    @GetMapping("/{organizationName}/{projectName}")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "GET",
        summary = "Get demo status.",
        description = "Get demo status.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched demo status.")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        authentication: Authentication,
    ): Mono<DemoStatus> = blockingToMono {
        projectService.findByNameAndOrganizationNameAndStatusIn(projectName, organizationName, setOf(ProjectStatus.CREATED))
    }
        .switchIfEmptyToNotFound {
            "$organizationName/$projectName has no demo linked with it."
        }
        .flatMap {
            lnkProjectGithubService.findByProject(it).toMono()
        }
        .flatMap {
            webClientDemo.get()
                .uri("/demo/internal/${it.githubOwner}/${it.githubRepoName}")
                .retrieve()
                .bodyToMono()
        }

}
