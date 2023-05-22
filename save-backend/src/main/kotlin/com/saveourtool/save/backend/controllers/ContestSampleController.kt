package com.saveourtool.save.backend.controllers

import com.saveourtool.save.backend.service.ContestSampleService
import com.saveourtool.save.entities.contest.ContestSampleDto
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.v1
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Controller for working with contests sample.
 */
@RestController
@RequestMapping(path = ["/api/$v1/contests/sample"])
class ContestSampleController(
    private val contestSampleService: ContestSampleService,
) {
    @PostMapping("/save")
    @Operation(
        method = "POST",
        summary = "Save contest sample.",
        description = "Save contest sample.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully saved contest sample")
    @PreAuthorize("permitAll()")
    fun save(
        @RequestBody contestSampleDto: ContestSampleDto,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        contestSampleService.save(contestSampleDto, authentication)
    }.map {
        ResponseEntity.ok("Contest sample was successfully saved")
    }
}
