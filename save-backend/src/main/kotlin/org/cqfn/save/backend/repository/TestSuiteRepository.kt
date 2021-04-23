package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.springframework.stereotype.Repository
import java.util.*

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite>
