package com.saveourtool.save.demo.controller

import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.service.DemoService
import com.saveourtool.save.demo.service.DownloadToolService
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

import java.nio.ByteBuffer
import java.time.LocalDateTime

import kotlinx.datetime.toKotlinLocalDateTime

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
    suspend fun listFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flow<FileDto> {
        val demo = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
            "Could not find demo for $organizationName/$projectName."
        }
        return dependencyStorage.list(demo, version)
            .asFlow()
            .map { dependency ->
                FileDto(
                    ProjectCoordinates(dependency.demo.organizationName, dependency.demo.projectName),
                    dependency.fileName,
                    LocalDateTime.now().toKotlinLocalDateTime(),
                )
            }
    }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of a file to be deleted
     * @param fileName name of a file to be deleted
     */
    @DeleteMapping("/{organizationName}/{projectName}/delete-file")
    suspend fun deleteFile(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestParam fileName: String,
    ) {
        val demo = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
            "Could not find demo for $organizationName/$projectName."
        }
        dependencyStorage.delete(demo, version, fileName)
    }

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version to attach files to
     * @param fileDtos list of [FileDto] containing information required for file download
     * @return [StringResponse] with response
     */
    @PostMapping("/{organizationName}/{projectName}/upload")
    suspend fun uploadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
        @RequestBody fileDtos: List<FileDto>,
    ): StringResponse {
        val demo = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
            "Could not find demo for $organizationName/$projectName."
        }
        return fileDtos.map { Dependency(demo, version, it.name, it.requiredId()) to it }
            .filterNot { (dependency, _) ->
                dependencyStorage.doesExist(dependency)
            }
            .map { (dependency, fileDto) ->
                downloadToolService.downloadToStorage(fileDto, dependency)
            }
            .toList()
            .let {
                it.size
            }
            .let { size ->
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
     * @return [Flow] of [ByteBuffer] - archive with files as content
     */
    @GetMapping("/{organizationName}/{projectName}/download-as-zip")
    suspend fun downloadFiles(
        @PathVariable organizationName: String,
        @PathVariable projectName: String,
        @RequestParam version: String,
    ): Flow<ByteBuffer> {
        val demo = demoService.findBySaveourtoolProjectOrNotFound(organizationName, projectName) {
            "Could not find demo for $organizationName/$projectName."
        }
        return dependencyStorage.archive(demo.organizationName, demo.projectName, version)
    }
        .flatMapMany { dependencyStorage.archive(it.organizationName, it.projectName, version) }

    companion object {
        private val log: Logger = getLogger<DependencyController>()
    }
}
