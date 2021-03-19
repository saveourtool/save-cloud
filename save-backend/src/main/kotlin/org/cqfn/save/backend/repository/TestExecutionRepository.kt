package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestExecution
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("MISSING_KDOC_TOP_LEVEL")
interface TestExecutionRepository : JpaRepository<TestExecution, Long>
