package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestSuite
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite>, JpaSpecificationExecutor<TestSuite>
