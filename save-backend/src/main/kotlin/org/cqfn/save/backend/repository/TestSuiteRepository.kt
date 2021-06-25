package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteType
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite> {
    /**
     * Find TestSuite by a set of fields
     *
     * @return test suite or null
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG")
    fun findByTypeAndNameAndProjectAndPropertiesRelativePath(
        type: TestSuiteType,
        name: String,
        project: Project,
        propertiesRelativePath: String,
    ): TestSuite?
}
