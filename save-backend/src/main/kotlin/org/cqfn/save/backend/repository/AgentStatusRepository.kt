package org.cqfn.save.backend.repository

import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaRepository

@Suppress("MISSING_KDOC_TOP_LEVEL")
interface AgentStatusRepository : JpaRepository<AgentStatus, Long>
