package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*
import com.saveourtool.save.utils.*

import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Internal controller that allows to create demos
 */
@RestController
@RequestMapping("/demo/internal/manager")
class ManagementController(
    private val downloadToolService: DownloadToolService,
    private val demoService: DemoService,
) {
    /**
     * @param demoDto
     * @return [Mono] of [DemoDto] entity
     */
    @PostMapping("/add")
    fun add(@RequestBody demoDto: DemoDto): Mono<DemoDto> = demoDto.toMono()
        .asyncEffect { downloadToolService.initializeGithubDownload(it.githubProjectCoordinates, it.vcsTagName) }
        .requireOrSwitchToResponseException({ validate() }, HttpStatus.CONFLICT) {
            "Demo creation request is invalid: fill project coordinates, run command and file name."
        }
        .flatMap {
            blockingToMono { demoService.saveIfNotPresent(it.toDemo()).toDto() }
        }

    /**
     * @param organizationName
     * @param projectName
     * @return [Mono] of [Unit]
     */
    @PostMapping("/{organizationName}/{projectName}/start")
    @Profile("kubernetes")
    fun start(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { demoService.start(it) }

    /**
     * @param organizationName
     * @param projectName
     * @return [Mono] of [Unit]
     */
    @PostMapping("/{organizationName}/{projectName}/stop")
    fun stop(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<Unit> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map { demoService.stop(it) }
}
