package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.NewDemoToolRequest
import com.saveourtool.save.demo.entity.GithubRepo
import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.demo.service.GithubDownloadToolService
import com.saveourtool.save.demo.service.GithubRepoService
import com.saveourtool.save.demo.service.SnapshotService
import com.saveourtool.save.demo.service.ToolService
import com.saveourtool.save.utils.blockingToMono
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
) {
    /**
     * @param newDemoToolRequest
     * @return [Mono] of [Tool] entity
     */
    @PostMapping("/add-tool")
    fun addTool(@RequestBody newDemoToolRequest: NewDemoToolRequest): Mono<Tool> = with(newDemoToolRequest) {
        GithubRepo(organizationName, projectName)
    }
        .let {
            blockingToMono {
                githubRepoService.saveIfNotPresent(it)
            }
        }
        .zipWhen { repo ->
            val vcsTagName = newDemoToolRequest.vcsTagName
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
            it
        }

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoStatus> = Mono.just(DemoStatus.STARTING)
}
