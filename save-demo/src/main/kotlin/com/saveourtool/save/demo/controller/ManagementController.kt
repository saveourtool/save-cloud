package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import com.saveourtool.save.utils.switchIfEmptyToResponseException

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

/**
 * Internal controller that allows to add tools to db
 */
@RestController
@RequestMapping("/demo/internal")
class ManagementController(
    private val toolService: ToolService,
    private val githubRepoService: GithubRepoService,
    private val snapshotService: SnapshotService,
    private val githubDownloadToolService: GithubDownloadToolService,
    private val demoService: DemoService,
) {
    /**
     * @param demoDto
     * @return [Mono] of [Tool] entity
     */
    @PostMapping("/add-tool")
    fun addTool(@RequestBody demoDto: DemoDto): Mono<DemoDto> = demoDto.githubProjectCoordinates
        ?.toGithubRepo()
        .let { repo ->
            blockingToMono {
                repo?.let {
                    githubRepoService.saveIfNotPresent(repo)
                }
            }
        }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            // todo: will be removed when uploading files will be implemented
            "Right now save-demo requires github repository to download tool. Please provide github repository."
        }
        .zipWhen { repo ->
            val vcsTagName = demoDto.vcsTagName
            Snapshot(vcsTagName, githubDownloadToolService.getExecutableName(repo, vcsTagName))
                .let {
                    blockingToMono {
                        snapshotService.saveIfNotPresent(it)
                    }
                }
        }
        .map { (repo, snapshot) ->
            toolService.saveIfNotPresent(repo, snapshot)
        }
        .map {
            githubDownloadToolService.downloadFromGithubAndUploadToStorage(it.githubRepo, it.snapshot.version)
        }
        .map {
            demoDto.also { demoService.saveIfNotPresent(it.toDemo()) }
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
    ): Mono<DemoStatus> = Mono.just(DemoStatus.STARTING)

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}")
    fun getDemoInfo(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoInfo> = blockingToMono {
        demoService.findBySaveourtoolProject(organizationName, projectName)
    }
        .switchIfEmptyToNotFound {
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
                    }
                    .orEmpty()
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
