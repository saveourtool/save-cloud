package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
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
    private val toolService: ToolService,
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
    fun start(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<Unit> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map {
            /*
             * todo:
             * kubernetesService.start(it)
             */
        }

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
        .map {
            /*
             * todo:
             * kubernetesService.stop(it)
             */
        }

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}/status")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoStatus> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .map {
            /*
             * todo:
             * kubernetesService.getStatus(it)
             */
            DemoStatus.STOPPED
        }

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}")
    fun getDemoInfo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoInfo> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .zipWith(getDemoStatus(organizationName, projectName))
        .map { (demo, status) ->
            DemoInfo(
                demo.toDto().copy(vcsTagName = ""),
                status,
            )
        }
        .zipWhen { demoInfo ->
            blockingToMono {
                demoInfo.demoDto
                    .githubProjectCoordinates
                    ?.let { repo ->
                        toolService.findCurrentVersion(repo)
                    } ?: "manual"
            }
        }
        .map { (demoInfo, currentVersion) ->
            demoInfo.copy(
                demoDto = demoInfo.demoDto.copy(
                    vcsTagName = currentVersion
                )
            )
        }
}
