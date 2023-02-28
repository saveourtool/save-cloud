package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.demo.service.DownloadToolService
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.downloadFromClasspath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

import java.nio.ByteBuffer
import java.time.LocalDateTime

import kotlinx.datetime.toKotlinLocalDateTime
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import reactor.core.publisher.Mono

/**
 * Internal controller that allows to upload files to save-demo
 */
@RestController
@RequestMapping("/demo/internal/files")
class DependencyController(
    private val demoService: DemoService,
    private val downloadToolService: DownloadToolService,
    private val dependencyStorage: DependencyStorage,
) {
    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version that the file should be marked with
     * @return [Flux] of [FileDto]s present in storage
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
     * @return [Mono] of [Unit]
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
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach files to
     * @param fileDtos list of [FileDto] containing information required for file download
     * @return [Mono] of [StringResponse] with response
     */
    @PostMapping("/{organizationName}/{projectName}/upload")
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
                StringResponse(
                    if (size == 0) {
                        "All files are already present in demo storage."
                    } else {
                        "Successfully saved $size files to demo storage."
                    },
                    HttpStatus.OK,
                )
            }
        }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach [zip] to
     * @return [Flux] of [ByteBuffer] - archive with files as content
     */
    @GetMapping("/{organizationName}/{projectName}/download-as-zip")
    fun downloadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flux<ByteBuffer> = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
        "Could not find demo for $organizationName/$projectName."
    }
        .flatMapMany { dependencyStorage.archive(it.organizationName, it.projectName, version) }

    /**
     * @return save-demo-agent.kexe as [Resource] wrapped into [Mono]
     *
     * todo: replace downloading form classpath with downloading from save-demo internal storage
     */
    @GetMapping("/download-agent", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadSaveAgent(): Mono<out Resource> = run {
        val executable = "save-demo-agent.kexe"

        downloadFromClasspath(executable) {
            "Can't find $executable on classpath"
        }
    }
}
