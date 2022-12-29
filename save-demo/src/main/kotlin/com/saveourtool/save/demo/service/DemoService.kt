package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.repository.DemoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * [Service] for [Demo] entity
 */
@Service
class DemoService(
    private val demoRepository: DemoRepository,
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
}
