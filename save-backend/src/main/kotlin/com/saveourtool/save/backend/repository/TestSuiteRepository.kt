package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuiteType
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite>, QueryByExampleExecutor<TestSuite> {
    /**
     * @param testSuiteType
     * @return list of test suites by type
     */
    fun findAllByTypeIs(testSuiteType: TestSuiteType): List<TestSuite>

    /**
     * @param testSuiteName name of the test suite
     * @param testSuiteType type of the test suite
     * @return list of test suites by name and type
     */
    fun findAllByNameAndType(testSuiteName: String, testSuiteType: TestSuiteType): List<TestSuite>

    /**
     * @param projectId id of the project associated with test suites
     * @return a list of test suites
     */
    fun findByProjectId(projectId: Long): List<TestSuite>

    /**
     * @param name name of the test suite
     * @param type type of the test suite
     * @param sourceId id of the source of the test suite
     * @return matched test suite
     */
    fun findByNameAndTypeAndSourceId(
        name: String, type: TestSuiteType, sourceId: Long,
    ): TestSuite

    /**
     * @param source source of the test suite
     * @return a list of test suites
     */
    fun findAllBySource(
        source: TestSuitesSource
    ): List<TestSuite>

    /**
     * @param sourceId ID of [TestSuitesSource]
     * @param version
     * @return list of [TestSuite]
     */
    fun findAllBySourceIdAndVersion(
        sourceId: Long,
        version: String
    ): List<TestSuite>
}
