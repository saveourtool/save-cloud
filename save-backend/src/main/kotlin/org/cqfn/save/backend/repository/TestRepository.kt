package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository of tests
 */
interface TestRepository : JpaRepository<Test, String>
