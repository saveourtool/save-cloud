package org.cqfn.save.entities

import java.time.LocalDateTime

@Entity
class TestStatus (
        var status: String,
        var agentId: String,
        var startTime: LocalDateTime,
        var endTime: LocalDateTime,
        @Id @GeneratedValue var id: Long? = null
)