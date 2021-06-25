package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
class TestSuitesService {
    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        val testSuites = testSuitesDto
            .map { TestSuite(it.type, it.name, it.project, LocalDateTime.now(), "save.properties") }
            .map { testSuite ->
                testSuiteRepository.findByTypeAndNameAndProjectAndPropertiesRelativePath(
                    testSuite.type!!, testSuite.name, testSuite.project!!, testSuite.propertiesRelativePath
                )
                    ?: testSuite
            }
        testSuites.filter { it.id == null }
            .let { testSuiteRepository.saveAll(it) }
        return testSuites.toList()
    }
}
