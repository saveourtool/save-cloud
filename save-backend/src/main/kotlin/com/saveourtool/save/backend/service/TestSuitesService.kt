package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.blockingToFlux
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteFilters
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.orNotFound
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.extra.math.max
import java.time.LocalDateTime

typealias TestSuiteDtoList = List<TestSuiteDto>

/**
 * Service for test suites
 */
@Service
@Suppress("LongParameterList")
class TestSuitesService(
    private val testSuiteRepository: TestSuiteRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
    private val testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage,
    private val executionService: ExecutionService,
    private val agentStatusService: AgentStatusService,
    private val agentService: AgentService,
) {
    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites **from the same source**, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Transactional
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        // FIXME: need to check logic about [dateAdded]
        // It's kind of upsert (insert or update) with key of all fields excluding [dateAdded]
        // This logic will be removed after https://github.com/saveourtool/save-cli/issues/429

        // test suites must be from the same source
        require(testSuitesDto.map { it.source.name to it.source.organizationName }.distinct().size == 1) {
            "Do not save test suites from different sources at the same time."
        }

        // test suites must be from the same commit
        require(testSuitesDto.map { it.version }.distinct().size == 1) {
            "Do not save test suites from different commits at the same time."
        }

        val testSuiteSourceVersion = testSuitesDto.map { it.version }.distinct().single()
        val testSuiteSource = testSuitesDto.first()
            .let { dto ->
                testSuitesSourceService.getByName(dto.source.organizationName, dto.source.name)
            }
            .apply {
                latestVersion = testSuiteSourceVersion
            }

        val testSuites = testSuitesDto
            .distinctBy {
                // Same suites may be declared in different directories, we unify them here.
                // We allow description of existing test suites to be changed.
                it.copy(description = null)
            }
            .map { dto ->
                TestSuite(
                    name = dto.name,
                    description = dto.description,
                    source = testSuiteSource,
                    version = dto.version,
                    dateAdded = null,
                    language = dto.language,
                    tags = dto.tags?.let(TestSuite::tagsFromList),
                    plugins = TestSuite.pluginsByTypes(dto.plugins)
                )
            }
            .map { testSuite ->
                // try to find TestSuite in the DB based on all non-null properties of `testSuite`
                // NB: that's why `dateAdded` is null in the mapping above
                val description = testSuite.description
                testSuiteRepository
                    .findOne(
                        Example.of(testSuite.apply { this.description = null })
                    )
                    .orElseGet {
                        // if testSuite is not present in the DB, we will save it with current timestamp
                        testSuite.apply {
                            dateAdded = LocalDateTime.now()
                            this.description = description
                        }
                    }
            }
        testSuiteRepository.saveAll(testSuites)
        testSuitesSourceService.update(testSuiteSource)
        return testSuites
    }

    /**
     * @return all standard test suites
     */
    fun getStandardTestSuites(): Mono<TestSuiteDtoList> = blockingToFlux { testSuitesSourceService.getStandardTestSuitesSources() }
        .flatMap { testSuitesSource ->
            testSuitesSourceSnapshotStorage.list(testSuitesSource.organization.name, testSuitesSource.name)
                .max { max, next -> max.creationTimeInMills.compareTo(next.creationTimeInMills) }
                .map { testSuitesSource to it.version }
        }
        .flatMap { (testSuitesSource, version) ->
            blockingToFlux { testSuiteRepository.findAllBySourceAndVersion(testSuitesSource, version) }
        }
        .map { it.toDto() }
        .collectList()

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long): TestSuite? = testSuiteRepository.findByIdOrNull(id)

    /**
     * @param ids
     * @return List of [TestSuite] by [ids]
     */
    fun findTestSuitesByIds(ids: List<Long>): List<TestSuite> = ids.mapNotNull { id ->
        testSuiteRepository.findByIdOrNull(id)
    }

    /**
     * @param organizationName
     * @return [List] of [TestSuite]s by [organizationName]
     */
    fun findTestSuitesByOrganizationName(organizationName: String): List<TestSuite> = testSuiteRepository.findBySourceOrganizationName(organizationName)

    /**
     * @return [List] of ALL [TestSuite]s
     */
    fun findAllTestSuites(): List<TestSuite> = testSuiteRepository.findAll()

    /**
     * @param filters
     * @return [List] of [TestSuite] that match [filters]
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    fun findTestSuitesMatchingFilters(filters: TestSuiteFilters): List<TestSuite> =
            ExampleMatcher.matchingAll()
                .withMatcher("name", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("language", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withMatcher("tags", ExampleMatcher.GenericPropertyMatchers.contains().ignoreCase())
                .withIgnorePaths("description", "source", "version", "dateAdded", "plugins")
                .let {
                    Example.of(
                        TestSuite(
                            filters.name,
                            "",
                            TestSuitesSource.empty,
                            "",
                            null,
                            filters.language,
                            filters.tags
                        ),
                        it
                    )
                }
                .let { testSuiteRepository.findAll(it) }

    /**
     * @param id
     * @return test suite with [id]
     * @throws ResponseStatusException if [TestSuite] is not found by [id]
     */
    fun getById(id: Long) = testSuiteRepository.findByIdOrNull(id)
        .orNotFound { "TestSuite (id=$id) not found" }

    /**
     * @param source source of the test suite
     * @param version version of snapshot of source
     * @return matched test suites
     */
    fun getBySourceAndVersion(
        source: TestSuitesSource,
        version: String
    ): List<TestSuite> = testSuiteRepository.findAllBySourceAndVersion(source, version)

    /**
     * @param source source of the test suite
     * @return matched test suites
     */
    fun getBySource(
        source: TestSuitesSource,
    ): List<TestSuite> = testSuiteRepository.findAllBySource(source)

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param testSuiteDtos suites, which need to be deleted
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        val testSuitesNamesAndIds = testSuiteDtos.map { it.name to getSavedIdByDto(it) }
        testSuitesNamesAndIds.forEach { (testSuiteName, testSuiteId) ->
            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.requiredId() }
            testIds.forEach { testId ->
                testExecutionRepository.findByTestId(testId).forEach { testExecution ->
                    // Delete test executions
                    val testExecutionId = testExecution.requiredId()
                    log.debug { "Delete test execution with id $testExecutionId" }
                    testExecutionRepository.deleteById(testExecutionId)
                }
                // Delete tests
                log.debug { "Delete test with id $testId" }
                testRepository.deleteById(testId)
            }
            log.info("Delete test suite $testSuiteName with id $testSuiteId")
            testSuiteRepository.deleteById(testSuiteId)
        }
        // Delete executions and agents, which related to the test suites
        // All test executions should be removed at this moment, that's why iterate one more time
        testSuitesNamesAndIds.forEach { (_, testSuiteId) ->
            val executionIds = executionService.getExecutionsByTestSuiteId(testSuiteId).map { it.id!! }
            agentStatusService.deleteAgentStatusWithExecutionIds(executionIds)
            agentService.deleteAgentByExecutionIds(executionIds)
            log.debug { "Delete executions with ids $executionIds" }
            executionService.deleteExecutionByIds(executionIds)
        }
    }

    /**
     * @param testSuiteIds IDs of [TestSuite]
     * @return a single version got from test suites
     */
    fun getSingleVersionByIds(testSuiteIds: List<Long>): String {
        require(testSuiteIds.isNotEmpty()) {
            "No test suite is selected"
        }
        val testSuites = testSuiteIds.map { getById(it) }
        testSuites.map { it.source }
            .distinctBy { it.requiredId() }
            .also { sources ->
                require(sources.size == 1) {
                    "Only a single test suites source is allowed for a run, but got: $sources"
                }
            }
        return testSuites.map { it.version }
            .distinct()
            .also { versions ->
                require(versions.size == 1) {
                    "Only a single version is supported, but got: $versions"
                }
            }
            .single()
    }

    private fun getSavedIdByDto(
        dto: TestSuiteDto,
    ): Long = testSuiteRepository.findByNameAndTagsAndSourceAndVersion(
        dto.name,
        dto.tags?.let(TestSuite::tagsFromList),
        testSuitesSourceService.getByName(dto.source.organizationName, dto.source.name),
        dto.version
    )
        ?.requiredId()
        .orNotFound { "TestSuite (name=${dto.name} in ${dto.source.name} with version ${dto.version}) not found" }

    companion object {
        private val log = LoggerFactory.getLogger(TestSuitesService::class.java)
    }
}
