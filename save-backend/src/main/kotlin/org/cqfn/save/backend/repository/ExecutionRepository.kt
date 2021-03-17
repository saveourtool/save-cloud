package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository of execution
 */
interface ExecutionRepository : JpaRepository<Execution, Long>
