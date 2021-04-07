package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import javax.persistence.Entity
import javax.persistence.Id

/**
 * @property agentId id of the agent, equals to id of the container, inside which the agent is running
 * @property executionId id of the execution, which the agent is serving
 */
@Entity
@Serializable
class Agent(
    @Id var agentId: String,
    var executionId: Long,
)
