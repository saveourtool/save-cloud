package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.demo.NewDemoToolRequest
import com.saveourtool.save.spring.utils.applyAll
import com.saveourtool.save.utils.EmptyResponse
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * Controller that allows adding tools to save-demo
 */
@RestController
@RequestMapping("/api/$v1/demo")
class DemoManagerController(
    configProperties: ConfigProperties,
    customizers: List<WebClientCustomizer>,
) {
    private val webClientDemo = WebClient.builder()
        .baseUrl(configProperties.demoUrl)
        .applyAll(customizers)
        .build()

    @PostMapping("/{organizationName}/{projectName}/add")
    @RequiresAuthorizationSourceHeader
    @PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
    @Operation(
        method = "POST",
        summary = "Add demo for a tool.",
        description = "Add demo for a tool.",
    )
    @Parameters(
        Parameter(name = "organizationName", `in` = ParameterIn.PATH, description = "name of an organization", required = true),
        Parameter(name = "projectName", `in` = ParameterIn.PATH, description = "name of a project", required = true),
    )
    @ApiResponse(responseCode = "200", description = "Successfully change status of a project.")
    fun addDemo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam vcsTagName: String,
        authentication: Authentication,
    ): Mono<EmptyResponse> = NewDemoToolRequest(organizationName, projectName, vcsTagName).toMono()
        .flatMap {
            webClientDemo.post()
                .uri("/demo/internal/add-tool")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(it)
                .retrieve()
                .toBodilessEntity()
        }
}
