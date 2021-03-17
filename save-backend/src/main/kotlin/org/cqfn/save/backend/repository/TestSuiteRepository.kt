package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestSuite
import org.springframework.data.jpa.repository.JpaRepository

interface TestSuiteRepository : JpaRepository<TestSuite, Long>
