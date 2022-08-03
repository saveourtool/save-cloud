package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite>, QueryByExampleExecutor<TestSuite> {
    /**
     * @param name name of the test suite
     * @param source source of the test suite
     * @param version version of snapshot of source
     * @return matched test suite
     */
    fun findByNameAndSourceAndVersion(
        name: String,
        source: TestSuitesSource,
        version: String
    ): TestSuite?

    /**
     * @param source source of the test suite
     * @param version version of snapshot of source
     * @return matched test suites
     */
    fun findAllBySourceAndVersion(
        source: TestSuitesSource,
        version: String
    ): List<TestSuite>
}
