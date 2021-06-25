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
    fun findByTypeAndNameAndProjectAndPropertiesRelativePath (
        type: TestSuiteType, name: String, project: Project, propertiesRelativePath: String,
    ): TestSuite?
}
