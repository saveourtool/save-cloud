package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteType
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

    fun findByNameAndTypeAndPropertiesRelativePathAndTestSuiteRepoUrl(name: String, type: TestSuiteType, propertiesRelPath: String, testSuiteRepoUrl: String): TestSuite
}
