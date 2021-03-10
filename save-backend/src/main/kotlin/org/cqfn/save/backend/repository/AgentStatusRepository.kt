package org.cqfn.save.backend.repository

import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaRepository

interface AgentStatusRepository : JpaRepository<AgentStatus, Long>