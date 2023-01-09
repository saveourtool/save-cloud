package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.repository.DemoRepository
import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.demo.storage.ToolStorage
import com.saveourtool.save.utils.overwrite
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * [Service] for [Demo] entity
 */
@Service
class DemoService(
    private val demoRepository: DemoRepository,
    private val toolStorage: ToolStorage,
) {
    private fun save(demo: Demo) = demoRepository.save(demo)

    /**
     * @return list of [Demo]s that are stored in database
     */
    fun getAllDemos(): List<Demo> = demoRepository.findAll()

    /**
     * @param demo
     * @return [Demo] entity saved to database
     * @throws IllegalStateException if [demo] is already present in DB
     */
    @Transactional
    fun saveIfNotPresent(demo: Demo): Demo = demoRepository.findByOrganizationNameAndProjectName(demo.organizationName, demo.projectName)?.let {
        throw IllegalStateException("Demo for project ${demo.organizationName}/${demo.projectName} is already added.")
    } ?: save(demo)

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @return [Demo] connected with project [organizationName]/[projectName] or null if not present
     */
    fun findBySaveourtoolProject(
        organizationName: String,
        projectName: String,
    ): Demo? = demoRepository.findByOrganizationNameAndProjectName(organizationName, projectName)

    /**
     * @param organizationName saveourtool organization name
     * @param projectName saveourtool project name
     * @param version version of a tool that the file is connected to
     * @param filePart file to be saved under required [version] in [organizationName]/[projectName]
     * @return amount of bytes loaded, wrapped into [Mono]
     */
    fun loadFileToStorage(
        organizationName: String,
        projectName: String,
        version: String,
        filePart: FilePart,
    ) = toolStorage.overwrite(
        ToolKey(organizationName, projectName, version, filePart.filename()),
        filePart,
    )
}
