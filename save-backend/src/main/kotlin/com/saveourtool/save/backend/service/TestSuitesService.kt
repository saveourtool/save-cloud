package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.filters.TestSuiteFilters
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.orNotFound

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

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
     * @param testSuiteDto test suite that should be checked and possibly saved
     * @return saved [TestSuite]
     */
    @Transactional
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuiteDto: TestSuiteDto): TestSuite {
        // FIXME: need to check logic about [dateAdded]
        // It's kind of upsert (insert or update) with key of all fields excluding [dateAdded]
        // This logic will be removed after https://github.com/saveourtool/save-cli/issues/429

        val testSuiteSourceVersion = testSuiteDto.version
        val testSuiteSource = testSuitesSourceService.getByName(testSuiteDto.source.organizationName, testSuiteDto.source.name)
            .apply {
                latestFetchedVersion = testSuiteSourceVersion
            }

        val testSuiteCandidate = TestSuite(
            name = testSuiteDto.name,
            description = testSuiteDto.description,
            source = testSuiteSource,
            version = testSuiteDto.version,
            dateAdded = null,
            language = testSuiteDto.language,
            tags = testSuiteDto.tags?.let(TestSuite::tagsFromList),
            plugins = TestSuite.pluginsByTypes(testSuiteDto.plugins)
        )
        // try to find TestSuite in the DB based on all non-null properties of `testSuite`
        // NB: that's why `dateAdded` is null in the mapping above
        val description = testSuiteCandidate.description
        val testSuite = testSuiteRepository
            .findOne(
                Example.of(testSuiteCandidate.apply { this.description = null })
            )
            .orElseGet {
                // if testSuite is not present in the DB, we will save it with current timestamp
                testSuiteCandidate.apply {
                    dateAdded = LocalDateTime.now()
                    this.description = description
                }
            }
        testSuiteRepository.save(testSuite)
        testSuitesSourceService.update(testSuiteSource)
        return testSuite
    }

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
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        doDeleteTestSuite(testSuiteDtos.map { getSavedEntityByDto(it) })
    }

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param source
     * @param version
     */
    fun deleteTestSuite(source: TestSuitesSource, version: String) {
        doDeleteTestSuite(testSuiteRepository.findAllBySourceAndVersion(source, version))
    }

    private fun doDeleteTestSuite(testSuites: List<TestSuite>) {
        testSuites.forEach { testSuite ->
            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuite.requiredId()).map { it.requiredId() }
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
            log.info("Delete test suite ${testSuite.name} with id ${testSuite.requiredId()}")
            testSuiteRepository.deleteById(testSuite.requiredId())
        }

        // Delete agents, which related to the test suites
        val executionIds = testSuites.flatMap { testSuite ->
            executionService.getExecutionsByTestSuiteId(testSuite.requiredId()).map { it.requiredId() }
        }.distinct()

        agentStatusService.deleteAgentStatusWithExecutionIds(executionIds)
        agentService.deleteAgentByExecutionIds(executionIds)

        executionIds.forEach {
            executionService.updateExecutionStatus(executionService.findExecution(it).orNotFound(), ExecutionStatus.OBSOLETE)
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

    private fun getSavedEntityByDto(
        dto: TestSuiteDto,
    ): TestSuite = testSuiteRepository.findByNameAndTagsAndSourceAndVersion(
        dto.name,
        dto.tags?.let(TestSuite::tagsFromList),
        testSuitesSourceService.getByName(dto.source.organizationName, dto.source.name),
        dto.version
    ).orNotFound { "TestSuite (name=${dto.name} in ${dto.source.name} with version ${dto.version}) not found" }

    companion object {
        private val log = LoggerFactory.getLogger(TestSuitesService::class.java)
    }
}
