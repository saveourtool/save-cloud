package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property containerId id of the container, inside which the agent is running
 * @property execution id of the execution, which the agent is serving
 * @property version
 */
@Entity
class Agent(
    var containerId: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    var version: String? = null,
) : BaseEntity() {
    /**
     * @return [AgentDto] from [Agent]
     */
    fun toDto(): AgentDto = AgentDto(
        containerId = containerId,
        executionId = execution.requiredId(),
        version = version,
    )
}

/**
 * @param executionResolver resolves [Execution] by [AgentDto.executionId]
 * @return [Agent] from [AgentDto]
 */
fun AgentDto.toEntity(executionResolver: (Long) -> Execution) = Agent(
    containerId = containerId,
    execution = executionResolver(executionId),
    version = version
)
