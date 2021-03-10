package org.cqfn.save.entities

import org.cqfn.save.agent.AgentState
import java.time.LocalDateTime
import javax.persistence.*

@Entity
class AgentStatus (
        @Column(name="time")
        var time: LocalDateTime,
        @Enumerated(EnumType.STRING)
        var state: AgentState,
        var agentId: String,
        @Id @GeneratedValue var id: Long? = null
)