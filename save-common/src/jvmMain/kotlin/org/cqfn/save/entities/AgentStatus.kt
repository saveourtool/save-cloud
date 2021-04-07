package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import org.cqfn.save.agent.AgentState
import org.cqfn.save.utils.LocalDateTimeSerializer
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * @property time
 * @property state
 * @property agentId
 * @property id
 */
@Entity
@Serializable
class AgentStatus(
    @Column(name = "time")
    @Serializable(with = LocalDateTimeSerializer::class)
    var time: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var state: AgentState,
    var agentId: String,
    @Id @GeneratedValue var id: Long? = null
)
