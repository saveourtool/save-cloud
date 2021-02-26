package org.cqfn.save.entities

import org.cqfn.save.agent.AgentState
import java.time.LocalDate
import javax.persistence.*

@Entity
class AgentStatus (
        @Column(name="time")
        var time: LocalDate,
        @Enumerated(EnumType.STRING)
        var state: AgentState,
        var agentId: Long,
        @Id @GeneratedValue var id: Long? = null
)