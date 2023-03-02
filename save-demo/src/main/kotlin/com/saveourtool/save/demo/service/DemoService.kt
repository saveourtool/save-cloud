package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.DemoStatus
import com.saveourtool.save.demo.entity.Demo
import com.saveourtool.save.demo.repository.DemoRepository
import com.saveourtool.save.demo.runners.RunnerFactory
import com.saveourtool.save.demo.storage.DependencyStorage
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.switchIfEmptyToNotFound

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono

import kotlinx.coroutines.reactor.mono

/**
 * [Service] for [Demo] entity
 */
@Service
class DemoService(
    private val demoRepository: DemoRepository,
    private val kubernetesService: KubernetesService?,
    private val dependencyStorage: DependencyStorage,
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
    fun start(demo: Demo): Mono<StringResponse> = kubernetesService?.let {
        kubernetesService.start(demo)
    } ?: Mono.fromCallable {
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
    fun getStatus(demo: Demo): Mono<DemoStatus> = kubernetesService?.let {
        mono { kubernetesService.getStatus(demo) }
    } ?: DemoStatus.RUNNING.toMono()

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
     * @return [Demo] or Mono filled with error (not found)
     */
    fun findBySaveourtoolProjectOrNotFound(
        organizationName: String,
        projectName: String,
        lazyMessage: () -> String,
    ): Mono<Demo> = blockingToMono {
        findBySaveourtoolProject(organizationName, projectName)
    }.switchIfEmptyToNotFound(lazyMessage)

    /**
     * @param demo [Demo] entity
     * @param version version of demo
     * @return [Mono] of [Unit]
     */
    fun delete(demo: Demo, version: String): Mono<StringResponse> = stop(demo)
        .let { dependencyStorage.list(demo, version) }
        .concatMap { dependencyStorage.delete(it) }
        .collectList()
        .publishOn(Schedulers.boundedElastic())
        .map { demoRepository.delete(demo) }
        .map { StringResponse.ok("Successfully deleted demo of ${demo.projectCoordinates()}.") }
}
