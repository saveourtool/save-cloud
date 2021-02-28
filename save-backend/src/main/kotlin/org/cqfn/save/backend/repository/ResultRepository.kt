package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Result
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for test result
 */
@Repository
interface ResultRepository : JpaRepository<Result, Int>
