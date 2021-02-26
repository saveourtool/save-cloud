package org.cqfn.save.entities

import org.cqfn.save.agent.AgentState
import java.sql.Date
import javax.persistence.*

@Entity
class AgentStatus (
        @Column(name="time")
        var time: Date,
        @Enumerated(EnumType.STRING)
        var state: AgentState,
        @Id @GeneratedValue var id: Long? = null
)