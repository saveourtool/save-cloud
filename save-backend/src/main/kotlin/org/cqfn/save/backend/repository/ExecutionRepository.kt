package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface ExecutionRepository : BaseEntityRepository<Execution>
