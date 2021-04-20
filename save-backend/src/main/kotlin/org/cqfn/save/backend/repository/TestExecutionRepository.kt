package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestExecution
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface TestExecutionRepository : BaseEntityRepository<TestExecution>
