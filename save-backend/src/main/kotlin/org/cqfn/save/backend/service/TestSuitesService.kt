package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.entities.TestSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for test suites
 */
@Service
class TestSuitesService {
    @Autowired
    private lateinit var baseEntityRepository: BaseEntityRepository<TestSuite>

    /**
     * @param testSuite
     */
    fun saveTestSuite(testSuite: TestSuite) {
        baseEntityRepository.save(testSuite)
    }
}
