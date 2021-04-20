/**
 * Data classes for agent status
 */

package org.cqfn.save.entities

import org.cqfn.save.agent.AgentState
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property time time of update
 * @property state current state of the agent
 * @property agent agent who's state is described
 */
@Entity
class AgentStatus(
    var time: LocalDateTime,

    @Enumerated(EnumType.STRING)
    var state: AgentState,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent,
) : BaseEntity()

/**
 * @property time time of update
 * @property state current state of the agent
 * @property containerId id of the agent's container
 */
class AgentStatusDto(
    val time: LocalDateTime,
    val state: AgentState,
    val containerId: String,
)
