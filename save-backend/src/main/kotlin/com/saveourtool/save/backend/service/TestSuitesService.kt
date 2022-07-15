package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testsuite.TestSuiteType
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.info
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Example
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
class TestSuitesService(
    private val testSuiteRepository: TestSuiteRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val testSuitesSourceService: TestSuitesSourceService,
) {
    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA", "UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        // FIXME: need to check logic about [dateAdded]
        val testSuites = testSuitesDto
            .distinctBy {
                // Same suites may be declared in different directories, we unify them here.
                // We allow description of existing test suites to be changed.
                it.copy(description = null)
            }
            .map {
                TestSuite(
                    name = it.name,
                    description = it.description,
                    source = testSuitesSourceService.getByDto(it.source),
                    version = it.version,
                    dateAdded = null,
                    language = it.language,
                    tags = it.tags?.let(TestSuite::tagsFromList),
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
        return testSuites.toList()
    }

    /**
     * @param dto entity as DTO
     * @return entity is found by provided values
     */
    fun getByDto(dto: TestSuiteDto): TestSuite = testSuiteRepository.findByNameAndSourceAndVersion(
        dto.name,
        testSuitesSourceService.getByDto(dto.source),
        dto.version
    ) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "TestSuite (name=${dto.name} in ${dto.source.name} with version ${dto.version}) not found")

    /**
     * @return all standard test suites
     */
    fun getStandardTestSuites() = testSuitesSourceService.findStandardTestSuitesSource()
        ?.let { testSuitesSource -> testSuiteRepository.findAllBySource(testSuitesSource).map { it.toDto() } }
        ?: emptyList()

    /**
     * @param name name of the test suite
     * @return all standard test suites with specific name
     */
    fun findStandardTestSuitesByName(name: String) = testSuitesSourceService.findStandardTestSuitesSource()
        ?.let { testSuitesSource -> testSuiteRepository.findAllBySource(testSuitesSource).filter { it.name == name } }
        ?: emptyList()

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long) = testSuiteRepository.findById(id)

    /**
     * @param id
     * @return test suite with [id]
     * @throws ResponseStatusException if [TestSuite] is not found by [id]
     */
    fun getById(id: Long) = testSuiteRepository.findByIdOrNull(id) ?:
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "TestSuite (id=$id) not found")

    @GetMapping("/internal/test-suites-source/{organizationName}/{name}/{version}/get-test-suites")
    fun findBySourceAndVersion(
        @PathVariable organizationName: String,
        @PathVariable name: String,
        @PathVariable version: String,
    ) = testSuitesSourceService.getByName(organizationName, name).map { testSuiteRepository.findAllBySourceAndVersion(it, version) }

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param testSuiteDtos suites, which need to be deleted
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            // Get test suite id by testSuiteDto
            val testSuiteId = getByDto(testSuiteDto).requiredId()

            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.requiredId() }
            testIds.forEach { testId ->
                // Executions could be absent
                testExecutionRepository.findByTestId(testId).ifPresent { testExecution ->
                    // Delete test executions
                    val testExecutionId = testExecution.requiredId()
                    log.debug { "Delete test execution with id $testExecutionId" }
                    testExecutionRepository.deleteById(testExecutionId)
                }
                // Delete tests
                log.debug { "Delete test with id $testId" }
                testRepository.deleteById(testId)
            }
            log.info("Delete test suite ${testSuiteDto.name} with id $testSuiteId")
            testSuiteRepository.deleteById(testSuiteId)
        }
    }




    companion object {
        private val log = LoggerFactory.getLogger(TestSuitesService::class.java)
    }
}
