package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.EmptyResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.domain.SourceSaveStatus
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.orNotFound

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * Service for [com.saveourtool.save.entities.TestSuitesSource]
 */
@Service
class TestSuitesSourceService(
    private val testSuitesSourceRepository: TestSuitesSourceRepository,
    private val organizationService: OrganizationService,
    private val gitService: GitService,
    configProperties: ConfigProperties,
    jackson2WebClientCustomizer: WebClientCustomizer,
) {
    private val preprocessorWebClient = WebClient.builder()
        .apply(jackson2WebClientCustomizer::customize)
        .baseUrl(configProperties.preprocessorUrl)
        .build()

    /**
     * @param organization [TestSuitesSource.organization]
     * @return list of entities of [TestSuitesSource] or null
     */
    fun getAllByOrganization(organization: Organization) =
            testSuitesSourceRepository.findAllByOrganizationId(organization.requiredId())

    /**
     * @param id [TestSuitesSource.id]
     * @return entity of [TestSuitesSource] or null
     */
    fun findById(id: Long): TestSuitesSource? = testSuitesSourceRepository.findByIdOrNull(id)

    /**
     * @param organization [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or null
     */
    fun findByName(organization: Organization, name: String) =
            testSuitesSourceRepository.findByOrganizationIdAndName(organization.requiredId(), name)

    /**
     * @param organizationName [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or null
     */
    fun findByName(organizationName: String, name: String) =
            testSuitesSourceRepository.findByOrganizationIdAndName(organizationService.getByName(organizationName).requiredId(), name)

    /**
     * @param organizationName [Organization.name] from [TestSuitesSource.organization]
     * @param name [TestSuitesSource.name]
     * @return entity of [TestSuitesSource] or error
     */
    fun getByName(organizationName: String, name: String): TestSuitesSource = findByName(organizationName, name)
        .orNotFound {
            "TestSuitesSource not found by name $name in $organizationName"
        }

    /**
     * @param git
     * @return entity
     */
    fun findByGit(git: Git) = testSuitesSourceRepository.findAllByGit(git)

    /**
     * @param entity
     */
    @Transactional
    fun delete(entity: TestSuitesSource) = testSuitesSourceRepository.delete(entity)

    /**
     * Raw update
     *
     * @param entity [TestSuitesSource] to be updated
     * @return status of updating [TestSuitesSource]
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    @Transactional
    @Transactional
    fun update(entity: TestSuitesSource): SourceSaveStatus {
        requireNotNull(entity.id) {
            "Cannot update entity as it is not saved yet: $this"
        }
        val isSaved = save(entity)
        return if (isSaved) SourceSaveStatus.UPDATED else SourceSaveStatus.CONFLICT
    }

    /**
     * @param entity
     * @return status of creating [TestSuitesSource]
     */
    @Transactional
    fun createSourceIfNotPresent(
        entity: TestSuitesSource,
    ): SourceSaveStatus = findByName(entity.organization, entity.name)?.let {
        SourceSaveStatus.EXIST
    } ?: run {
        val isSaved = save(entity)
        if (isSaved) SourceSaveStatus.NEW else SourceSaveStatus.CONFLICT
    }

    private fun save(entity: TestSuitesSource) = try {
        testSuitesSourceRepository.save(entity)
        true
    } catch (e: DataIntegrityViolationException) {
        false
    }

    /**
     * @return list of [TestSuitesSource] for STANDARD tests or empty
     */
    @Transactional
    fun getStandardTestSuitesSources(): List<TestSuitesSource> {
        // FIXME: a hardcoded values for standard test suites
        // Will be removed in phase 3
        val organizationName = "CQFN.org"
        val gitUrl = "https://github.com/saveourtool/save-cli"
        val testRootPaths = listOf("examples/kotlin-diktat", "examples/discovery-test")
        val organization = organizationService.getByName(organizationName)
        val git = gitService.getByOrganizationAndUrl(organization, gitUrl)
        return testRootPaths.map { testRootPath ->
            testSuitesSourceRepository.findByOrganizationAndGitAndTestRootPath(
                organization,
                git,
                testRootPath
            ) ?: testSuitesSourceRepository.save(TestSuitesSource(
                organization = organization,
                name = "Standard-${testRootPath.removePrefix("examples/")}",
                description = "Standard test suites from $organizationName: $testRootPath",
                git = git,
                testRootPath = testRootPath,
                latestFetchedVersion = null,
            ))
        }
    }

    /**
     * @return list of organizations that have open public test suite sources
     */
    fun getAvailableTestSuiteSources(): List<TestSuitesSource> = testSuitesSourceRepository.findAll()

    /**
     * @param testSuitesSource test suites source which requested to be fetched
     * @return empty response
     */
    fun fetch(
        testSuitesSource: TestSuitesSourceDto,
    ): Mono<EmptyResponse> = preprocessorWebClient.post()
        .uri("/git/tag-list")
        .bodyValue(testSuitesSource.gitDto)
        .retrieve()
        .bodyToMono<List<String>>()
        .flatMapIterable { it }
        .flatMap { tagName ->
            preprocessorWebClient.post()
                .uri("/test-suites-sources/fetch-from-tag?tagName={tagName}", tagName)
                .bodyValue(testSuitesSource)
                .retrieve()
                .toBodilessEntity()
        }
        .collectList()
        .flatMap {
            preprocessorWebClient.post()
                .uri("/git/default-branch-name")
                .bodyValue(testSuitesSource.gitDto)
                .retrieve()
                .bodyToMono<String>()
        }
        .flatMap { branchName ->
            preprocessorWebClient.post()
                .uri("/test-suites-sources/fetch-from-branch?branchName={branchName}", branchName)
                .bodyValue(testSuitesSource)
                .retrieve()
                .toBodilessEntity()
        }
}
