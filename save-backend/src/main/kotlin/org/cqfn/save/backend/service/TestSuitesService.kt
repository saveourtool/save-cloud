package org.cqfn.save.backend.service

import org.cqfn.save.backend.controllers.TestController
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
            .map { TestSuite(it.type, it.name, it.project, null, it.propertiesRelativePath, it.testSuiteRepoUrl) }
            .map { testSuite ->
                // try to find TestSuite in the DB based on all non-null properties of `testSuite`
                // NB: that's why `dateAdded` is null in the mapping above
                testSuiteRepository.findOne(
                    Example.of(testSuite)
                )
                    // if testSuite is not present in the DB, we will save it with current timestamp
                    .orElseGet {
                        testSuite.apply {
                            dateAdded = LocalDateTime.now()
                        }
                    }
            }
        testSuites.filter { it.id == null }
            .let { testSuiteRepository.saveAll(it) }
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

    fun deleteTestSuiteDto(testSuiteDto: TestSuiteDto) {
        // Get test suite id by test dto
        log.info("\n\nCHECK: ${testSuiteDto.type!!} | ${testSuiteDto.testSuiteRepoUrl!!}")

        val testSuiteId = testSuiteRepository.findByNameAndTypeAndPropertiesRelativePathAndTestSuiteRepoUrl(
            testSuiteDto.name,
            testSuiteDto.type!!,
            testSuiteDto.propertiesRelativePath,
            testSuiteDto.testSuiteRepoUrl!!,
        ).id!!
        log.info("CHECK2 ${testSuiteId}")

        // Get test ids related to the current testSuiteId
        val testIds = testRepository.findAllByTestSuiteId(testSuiteId).map { it.id }
        testIds.forEach { id ->
            // Executions could be absent
            testExecutionRepository.findByTestId(id!!).ifPresent { testExecution ->
                // Delete test executions
                log.info("\n\nDELETE TEST EXECUTION WITH TEST ID ${id}")
                testExecutionRepository.deleteById(testExecution.id!!)
            }

            // Delete tests
            log.info("\n\nDELETE TESTS WITH ID ${id}")
            testRepository.deleteById(id)
        }

        log.info("DELETE  TEST SUITE WITH ID ${testSuiteId}")
        testSuiteRepository.deleteById(testSuiteId)

    }

    companion object {
        private val log = LoggerFactory.getLogger(TestController::class.java)
    }
}
