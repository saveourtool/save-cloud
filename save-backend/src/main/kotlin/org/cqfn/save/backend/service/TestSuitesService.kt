package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.TestSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for test suites
 */
@Service
class TestSuitesService {
    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    /**
     * @param testSuite
     */
    fun saveTestSuite(testSuite: TestSuite) {
        testSuiteRepository.save(testSuite)
    }
}
