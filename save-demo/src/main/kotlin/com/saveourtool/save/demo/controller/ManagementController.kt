package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*
import com.saveourtool.save.utils.*

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
    suspend fun add(@RequestBody demoDto: DemoDto): DemoDto {
        downloadToolService.initializeGithubDownload(demoDto.githubProjectCoordinates, demoDto.vcsTagName)
        return demoDto.takeIf { it.validate() }
            .orConflict {
                "Demo creation request is invalid: fill project coordinates, run command and file name."
            }
            .let { validatedDemoDto ->
                withContext(Dispatchers.IO) {
                    demoService.saveIfNotPresent(validatedDemoDto.toDemo())
                }
            }
            .toDto()
    }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of demo
     * @return [Mono] of [StringResponse]
     */
    @PostMapping("/{organizationName}/{projectName}/delete")
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam(required = false, defaultValue = "manual") version: String,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { demoService.delete(it, version) }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return [Mono] of [StringResponse]
     */
    @PostMapping("/{organizationName}/{projectName}/start")
    suspend fun start(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): StringResponse = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .let { demoService.start(it) }

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
