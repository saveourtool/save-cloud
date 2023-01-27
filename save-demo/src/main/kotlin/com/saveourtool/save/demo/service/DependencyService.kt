package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.repository.DependencyRepository
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.utils.asyncEffect
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

/**
 * [Service] for [Dependency] entity
 * @property dependencyRepository
 * @property toolStorage
 */
@Service
class DependencyService(
    val dependencyRepository: DependencyRepository,
    val toolStorage: ToolStorage,
) {
    /**
     * Save list of [Dependency], excluding present ones
     *
     * @param dependencies list of [Dependency] to save to database
     * @return [dependencies] saved to database
     */
    fun saveIfNotPresent(dependencies: List<Dependency>): List<Dependency> = dependencies.firstOrNull()
        ?.let {
            dependencyRepository.findAllByDemoAndVersion(it.demo, it.version)
        }
        ?.let { presentDependencies ->
            dependencies.filter { dependency ->
                dependency.fileId !in presentDependencies.map { it.fileId }
            }
        }
        ?.let {
            dependencyRepository.saveAll(it)
        }
        .orEmpty()

    /**
     * Save [fileDtos] linked with [demo]'s [version]
     *
     * @param demo
     * @param version string that represents the version of required file
     * @param fileDtos list of [FileDto] filled with information required for [Dependency] downloading
     * @return [fileDtos] as list of [Dependency] saved to database
     */
    fun saveDependencies(demo: Demo, version: String, fileDtos: List<FileDto>): List<Dependency> = fileDtos.map {
        Dependency(demo, version, it.name, it.requiredId())
    }.let { saveIfNotPresent(it) }

    private fun deleteFromDbByToolKey(
        toolKey: ToolKey
    ) = with(toolKey) {
        dependencyRepository.deleteByDemoOrganizationNameAndDemoProjectNameAndVersionAndFileName(
            organizationName,
            projectName,
            version,
            fileName,
        )
    }

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun getDependencies(
        demo: Demo,
        version: String,
    ) = dependencyRepository.findAllByDemoAndVersion(demo, version)

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @return list of files present in storage for required version
     */
    fun getDependenciesAsToolKeys(
        demo: Demo,
        version: String,
    ) = getDependencies(demo, version).map { dependency ->
        with(dependency) { ToolKey(demo.organizationName, demo.projectName, version, fileName) }
    }

    /**
     * @param demo
     * @param version version of a tool that the file is connected to
     * @param fileName name of a file to be deleted
     * @return true if file is successfully deleted, false otherwise
     */
    fun deleteDependency(
        demo: Demo,
        version: String,
        fileName: String,
    ): Mono<Unit> = ToolKey(demo.organizationName, demo.projectName, version, fileName).toMono()
        .asyncEffect { toolStorage.delete(it) }
        .map { deleteFromDbByToolKey(it) }
}
