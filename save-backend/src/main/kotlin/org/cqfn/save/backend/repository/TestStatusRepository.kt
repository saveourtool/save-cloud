package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface TestStatusRepository : JpaRepository<TestStatus, Long>