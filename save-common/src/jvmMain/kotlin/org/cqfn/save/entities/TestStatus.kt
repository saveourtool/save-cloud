package org.cqfn.save.entities

import org.cqfn.save.test_status.TestResultStatus
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Entity
class TestStatus (
        @Enumerated(EnumType.STRING)
        var status: TestResultStatus,
        var agentId: String,
        var startTime: LocalDateTime,
        var endTime: LocalDateTime,
        @Id @GeneratedValue var id: Long? = null
)