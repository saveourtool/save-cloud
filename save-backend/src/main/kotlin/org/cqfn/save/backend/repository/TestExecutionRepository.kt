package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestExecution
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface TestExecutionRepository : BaseEntityRepository<TestExecution>,
JpaSpecificationExecutor<TestExecution>
