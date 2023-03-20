package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoAgentConfig
import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.repository.DemoRepository
import com.saveourtool.save.demo.runners.RunnerFactory
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.utils.*

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * [Service] for [Demo] entity
 */
@Service
class DemoService(
    private val demoRepository: DemoRepository,
    private val kubernetesService: KubernetesService?,
    private val dependencyStorage: DependencyStorage,
    private val blockingBridge: BlockingBridge,
) {
    /**
     * Get preferred [RunnerFactory.RunnerType] for demo runner.
     *
     * @return preferred [RunnerFactory.RunnerType] for demo runner.
     */
    fun getRunnerType(): Mono<RunnerFactory.RunnerType> = Mono.fromCallable {
        kubernetesService?.let { RunnerFactory.RunnerType.POD } ?: RunnerFactory.RunnerType.CLI
    }

    /**
     * Start kubernetes job if kubernetes profile is set, otherwise do nothing
     *
     * @param demo demo entity
     * @return [Mono] of [StringResponse] filled with readable message
     */
    suspend fun start(demo: Demo): StringResponse = kubernetesService?.let {
        kubernetesService.start(demo)
    } ?: run {
        StringResponse.ok("Demo successfully created")
    }

    /**
     * Stop kubernetes job if kubernetes profile is set, do nothing otherwise
     *
     * @param demo demo entity
     */
    fun stop(demo: Demo) = kubernetesService?.let { kubernetesService.stop(demo) } ?: Unit

    /**
     * Get [DemoStatus] of [demo]
     *
     * If kubernetes profile is enabled, request is performed in order to get current status
     * If kubernetes profile is not enabled, [DemoStatus.RUNNING] is returned
     *
     * @param demo demo entity
     * @return current [DemoStatus] of [demo]
     */
    suspend fun getStatus(demo: Demo): DemoStatus = kubernetesService?.let {
        kubernetesService.getStatus(demo)
    } ?: DemoStatus.RUNNING

    /**
     * Get save-demo-agent configuration
     *
     * @param demo [Demo] entity
     * @param version required demo version
     * @return [DemoAgentConfig] corresponding to [Demo] with [version]
     * @throws IllegalStateException on inactive kubernetes profile
     */
    fun getAgentConfiguration(
        demo: Demo,
        version: String,
    ) = kubernetesService?.getConfiguration(demo, version) ?: throw IllegalStateException(
        "Could not get configuration for pod as kubernetes profile is inactive."
    )

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
     * @param lazyMessage
     * @return [Demo] or error (not found)
     */
    suspend fun findBySaveourtoolProjectOrNotFound(
        organizationName: String,
        projectName: String,
        lazyMessage: () -> String,
    ): Demo = blockingBridge.blockingToSuspend {
        findBySaveourtoolProject(organizationName, projectName)
            .orNotFound(lazyMessage)
    }

    /**
     * @param demo [Demo] entity
     * @param version version of demo
     * @return [Mono] of [Unit]
     */
    suspend fun delete(demo: Demo, version: String): StringResponse {
        stop(demo)
        dependencyStorage.list(demo, version)
            .onEach {
                dependencyStorage.delete(it)
            }
        blockingBridge.blockingToSuspend {
            demoRepository.delete(demo)
        }
        return StringResponse.ok("Successfully deleted demo of ${demo.projectCoordinates()}.")
    }
}
