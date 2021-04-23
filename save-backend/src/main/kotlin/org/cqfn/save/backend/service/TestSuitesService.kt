package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Service for test suites
 */
@Service
class TestSuitesService {
    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    /**
     * @param testSuitesDto
     */
    fun saveTestSuite(testSuitesDto: List<TestSuiteDto>): List<TestSuite> {
        val testSuites = testSuitesDto.map { TestSuite(it.type, it.name, it.project, LocalDateTime.now()) }
        testSuiteRepository.saveAll(testSuites)
        return testSuites
    }
}
