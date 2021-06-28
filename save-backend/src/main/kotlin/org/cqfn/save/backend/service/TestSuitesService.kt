package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
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

    /**
     * Save new test suites to DB
     *
     * @param testSuitesDto test suites, that should be checked and possibly saved
     * @return list of *all* TestSuites
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        val testSuites = testSuitesDto
            .map { TestSuite(it.type, it.name, it.project, null, it.propertiesRelativePath) }
            .map { testSuite ->
                // try to find TestSuite in the DB based on all non-null properties of `testSuite`
                // NB: that's why `dateAdded` is null in the mapping above
                testSuiteRepository.findOne(
                    Example.of(testSuite)
                )
                    // if testSuite is not present in the DB, we will save it with current timestamp
                    .orElse(testSuite.apply {
                        dateAdded = LocalDateTime.now()
                    })
            }
        testSuites.filter { it.id == null }
            .let { testSuiteRepository.saveAll(it) }
        return testSuites.toList()
    }
}
