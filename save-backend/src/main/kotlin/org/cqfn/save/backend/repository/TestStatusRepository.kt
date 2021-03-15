package org.cqfn.save.backend.repository

import org.cqfn.save.entities.TestStatus
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("MISSING_KDOC_TOP_LEVEL")
interface TestStatusRepository : JpaRepository<TestStatus, Long>
