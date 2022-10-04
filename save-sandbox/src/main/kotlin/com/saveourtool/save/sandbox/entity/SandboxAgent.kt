package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.entities.AgentDto
import com.saveourtool.save.spring.entity.BaseEntity

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property containerId id of the container, inside which the agent is running
 * @property containerName name of the container, inside which the agent is running
 * @property execution id of the execution, which the agent is serving
 * @property version
 */
@Entity
@Table(name = "agent")
class SandboxAgent(
    var containerId: String,
    var containerName: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "execution_id")
    var execution: SandboxExecution,

    var version: String? = null,
) : BaseEntity() {
    /**
     * @return [AgentDto] from [Agent]
     */
    fun toDto(): AgentDto = AgentDto(
        containerId = containerId,
        containerName = containerName,
        executionId = execution.requiredId(),
        version = version,
    )
}

/**
 * @param executionResolver resolves [SandboxExecution] by [AgentDto.executionId]
 * @return [SandboxAgent] from [AgentDto]
 */
fun AgentDto.toEntity(executionResolver: (Long) -> SandboxExecution) = SandboxAgent(
    containerId = containerId,
    containerName = containerName,
    execution = executionResolver(executionId),
    version = version
)
