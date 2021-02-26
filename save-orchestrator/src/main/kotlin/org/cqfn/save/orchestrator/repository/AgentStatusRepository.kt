package org.cqfn.save.orchestrator.repository

import org.cqfn.save.entities.AgentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AgentStatusRepository: JpaRepository<AgentStatus, Long>