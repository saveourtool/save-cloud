package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.entities.Agent
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property execution execution that is connected to [agent]
 * @property agent agent assigned to [execution]
 */
@Entity
@Table(name = "lnk_execution_agent")
class SandboxLnkExecutionAgent(
    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: SandboxExecution,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent,
) : BaseEntity()
