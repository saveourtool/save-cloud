package org.cqfn.save.entities

import org.cqfn.save.agent.AgentState
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property time
 * @property state
 * @property agent
 */
@Entity
@Table(name = "agent_status")
class AgentStatus(

    @Column(name = "time")
    var time: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    var state: AgentState,

    @JoinColumn(name = "id")
    @ManyToOne
    @Column(name = "agent_id")
    var agent: Agent,

) : BaseEntity()
