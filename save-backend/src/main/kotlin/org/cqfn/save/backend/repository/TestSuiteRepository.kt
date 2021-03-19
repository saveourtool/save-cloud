package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestSuite
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository of test suites
 */
interface TestSuiteRepository : JpaRepository<TestSuite, Long>
