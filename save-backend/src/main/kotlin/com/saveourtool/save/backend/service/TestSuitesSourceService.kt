package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.EmptyResponse
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.domain.SourceSaveStatus
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSource.Companion.toTestSuiteSource
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.orNotFound

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
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
     * @param entity
     * @return saved [TestSuitesSource] with values from provided [entity]
     */
    @Transactional
    fun save(
        entity: TestSuitesSourceDto
    ): TestSuitesSource = organizationService.getByName(entity.organizationName)
        .let { organization ->
            val git = gitService.getByOrganizationAndUrl(organization, entity.gitDto.url)
            testSuitesSourceRepository.save(entity.toTestSuiteSource(organization, git))
        }

    /**
     * @param organization
     * @param git
     * @param testRootPath
     * @param branch
     * @return existed [TestSuitesSourceDto] with provided values or created a new one as auto-generated entity
     */
    @Transactional
    fun getOrCreate(
        organization: Organization,
        git: Git,
        testRootPath: String,
        branch: String,
    ): TestSuitesSource = testSuitesSourceRepository.findByOrganizationAndGitAndBranchAndTestRootPath(
        organization,
        git,
        branch,
        testRootPath
    ) ?: createAutoGenerated(organization, git, testRootPath, branch)

    /**
     * @param testSuitesSourceRequest
     * @return saved [TestSuitesSource]
     */
    fun createSourceIfNotPresent(
        testSuitesSourceRequest: TestSuitesSource,
    ): SourceSaveStatus = findByName(testSuitesSourceRequest.organization, testSuitesSourceRequest.name)?.let {
        SourceSaveStatus.EXIST
    } ?: run {
        try {
            testSuitesSourceRepository.save(testSuitesSourceRequest)
        } catch (e: DataIntegrityViolationException) {
            return SourceSaveStatus.CONFLICT
        }
        SourceSaveStatus.NEW
    }

    private fun createAutoGenerated(
        organization: Organization,
        git: Git,
        testRootPath: String,
        branch: String,
    ) = save(
        TestSuitesSourceDto(
            organizationName = organization.name,
            name = generateDefaultName(organization.requiredId()),
            description = "auto created test suites source by git coordinates",
            gitDto = git.toDto(),
            branch = branch,
            testRootPath = testRootPath,
        )
    )

    /**
     * @return list of [TestSuitesSource] for STANDARD tests or empty
     */
    @Transactional
    fun getStandardTestSuitesSources(): List<TestSuitesSource> {
        // FIXME: a hardcoded values for standard test suites
        // Will be removed in phase 3
        val organizationName = "CQFN.org"
        val gitUrl = "https://github.com/saveourtool/save-cli"
        val branch = "main"
        val testRootPaths = listOf("examples/kotlin-diktat", "examples/discovery-test")
        return testRootPaths.map { testRootPath ->
            getOrCreate(
                organizationService.getByName(organizationName),
                gitService.getByOrganizationAndUrl(organizationService.getByName(organizationName), gitUrl),
                testRootPath,
                branch,
            )
        }
    }

    /**
     * @return list of organizations that have open public test suite sources
     */
    fun getAvaliableTestSuiteSources(): List<TestSuitesSource> = testSuitesSourceRepository.findAll()

    /**
     * @param testSuitesSource test suites source which requested to be fetched
     * @return empty response
     */
    fun fetch(
        testSuitesSource: TestSuitesSourceDto,
    ): Mono<EmptyResponse> = preprocessorWebClient.post()
        .uri("/test-suites-sources/fetch")
        .bodyValue(testSuitesSource)
        .retrieve()
        .toBodilessEntity()

    private fun generateDefaultName(organizationId: Long): String =
            "TestSuitesSource-${testSuitesSourceRepository.findAllByOrganizationId(organizationId).size + 1}"
}
