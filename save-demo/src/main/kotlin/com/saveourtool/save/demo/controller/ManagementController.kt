package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.DemoDto
import com.saveourtool.save.demo.DemoInfo
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.entity.*
import com.saveourtool.save.demo.service.*
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.utils.*

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

import java.nio.ByteBuffer
import java.time.LocalDateTime

import kotlinx.datetime.toKotlinLocalDateTime

/**
 * Internal controller that allows to create demos
 */
@RestController
@RequestMapping("/demo/internal")
class ManagementController(
    private val toolService: ToolService,
    private val downloadToolService: DownloadToolService,
    private val demoService: DemoService,
    private val dependencyStorage: DependencyStorage,
) {
    /**
     * @param demoDto
     * @return [Mono] of [DemoDto] entity
     */
    @PostMapping("/add-tool")
    fun addTool(@RequestBody demoDto: DemoDto): Mono<DemoDto> = demoDto.toMono()
        .asyncEffect { downloadToolService.initializeGithubDownload(it.githubProjectCoordinates, it.vcsTagName) }
        .requireOrSwitchToResponseException({ validate() }, HttpStatus.CONFLICT) {
            "Demo creation request is invalid: fill project coordinates, run command and file name."
        }
        .asyncEffect { blockingToMono { demoService.saveIfNotPresent(it.toDemo()) } }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version
     * @return [Flux] of files present in storage
     */
    @GetMapping("/{organizationName}/{projectName}/list-file")
    fun listFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flux<FileDto> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapMany {
            dependencyStorage.list(it, version)
        }
        .map { dependency ->
            FileDto(
                ProjectCoordinates(dependency.demo.organizationName, dependency.demo.projectName),
                dependency.fileName,
                LocalDateTime.now().toKotlinLocalDateTime(),
            )
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of a file to be deleted
     * @param fileName name of a file to be deleted
     * @return true if file is successfully deleted, false otherwise
     */
    @DeleteMapping("/{organizationName}/{projectName}/delete-file")
    fun deleteFile(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestParam fileName: String,
    ): Mono<Unit> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMap { dependencyStorage.delete(it, version, fileName) }

    /**
     * @param organizationName name of GitHub user/organization
     * @param projectName name of GitHub repository
     * @return [Mono] of [DemoStatus] of current demo
     */
    @GetMapping("/{organizationName}/{projectName}/status")
    fun getDemoStatus(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
    ): Mono<DemoStatus> = Mono.just(DemoStatus.STOPPED)

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

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach files to
     * @param fileDtos list of [FileDto] containing information required for file download
     * @return [StringResponse] with response
     */
    @PostMapping("/{organizationName}/{projectName}/upload-files")
    fun uploadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestBody fileDtos: List<FileDto>,
    ): Mono<StringResponse> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapIterable { demo ->
            fileDtos.map { Dependency(demo, version, it.name, it.requiredId()) }
        }
        .filterWhen {
            dependencyStorage.doesExist(it).map(Boolean::not)
        }
        .collectList()
        .map { dependencies ->
            downloadToolService.downloadToStorage(dependencies).let { size ->
                StringResponse("Successfully saved $size files to demo storage.", HttpStatus.OK)
            }
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach [zip] to
     * @return [Flux] of [ByteBuffer] - archive with files as content
     */
    @GetMapping("/{organizationName}/{projectName}/download-files-as-zip")
    fun downloadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flux<ByteBuffer> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapMany { dependencyStorage.archive(it.organizationName, it.projectName, version) }
}
