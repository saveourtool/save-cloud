package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Example
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
class TestSuitesService {
    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        val testSuites = testSuitesDto
            .distinctBy {
                // Same suites may be declared in different directories, we unify them here.
                // We allow description of existing test suites to be changed.
                it.copy(description = null)
            }
            .map {
                TestSuite(it.type, it.name, it.description, it.project, null, it.testRootPath, it.testSuiteRepoUrl)
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
     * @return all standard test suites
     */
    fun getStandardTestSuites() =
            testSuiteRepository.findAllByTypeIs(TestSuiteType.STANDARD).map { it.toDto() }

    /**
     * @param name name of the test suite
     * @return all standard test suites with specific name
     */
    fun findStandardTestSuitesByName(name: String) =
            testSuiteRepository.findAllByNameAndType(name, TestSuiteType.STANDARD)

    /**
     * @param project a project associated with test suites
     * @return a list of test suites
     */
    fun findTestSuitesByProject(project: Project) =
            testSuiteRepository.findByProjectId(
                requireNotNull(project.id) { "Cannot find test suites for project with missing id (name=${project.name}, owner=${project.owner})" }
            )

    /**
     * @param id
     * @return test suite with [id]
     */
    fun findTestSuiteById(id: Long) = testSuiteRepository.findById(id)

    /**
     * Mark provided testSuites as obsolete
     *
     * @param testSuiteDtos
     */
    @Suppress("UnsafeCallOnNullableType")
    fun markObsoleteTestSuites(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            val testSuite = testSuiteRepository.findByNameAndTypeAndTestRootPathAndTestSuiteRepoUrl(
                testSuiteDto.name,
                testSuiteDto.type!!,
                testSuiteDto.testRootPath,
                testSuiteDto.testSuiteRepoUrl,
            )
            log.info("Mark test suite ${testSuite.name} with id ${testSuite.id} as obsolete")
            testSuite.type = TestSuiteType.OBSOLETE_STANDARD
            testSuiteRepository.save(testSuite)
        }
    }

    /**
     * Delete testSuites and related tests & test executions from DB
     *
     * @param testSuiteDtos suites, which need to be deleted
     */
    @Suppress("UnsafeCallOnNullableType")
    fun deleteTestSuiteDto(testSuiteDtos: List<TestSuiteDto>) {
        testSuiteDtos.forEach { testSuiteDto ->
            // Get test suite id by testSuiteDto
            val testSuiteId = testSuiteRepository.findByNameAndTypeAndTestRootPathAndTestSuiteRepoUrl(
                testSuiteDto.name,
                testSuiteDto.type!!,
                testSuiteDto.testRootPath,
                testSuiteDto.testSuiteRepoUrl,
            ).id!!

            // Get test ids related to the current testSuiteId
            val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.id }
            testIds.forEach { testId ->
                // Executions could be absent
                testExecutionRepository.findByTestId(testId!!).ifPresent { testExecution ->
                    // Delete test executions
                    log.debug("Delete test execution with id ${testExecution.id}")
                    testExecutionRepository.deleteById(testExecution.id!!)
                }
                // Delete tests
                log.debug("Delete test with id $testId")
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
