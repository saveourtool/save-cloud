package org.cqfn.save.entities

@Entity
class AgentStatus (
        var time: String, // Fixme. Should be Date or smth
        var status: AgentStatus,
        @Id @GeneratedValue id: Long? = null
)