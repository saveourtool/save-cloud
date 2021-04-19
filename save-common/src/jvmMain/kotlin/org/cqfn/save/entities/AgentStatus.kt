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
 * @property time
 * @property state
 * @property agent
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
 * @property time
 * @property state
 * @property containerId
 */
class AgentStatusDto(
    val time: LocalDateTime,
    val state: AgentState,
    val containerId: String,
)
