package org.cqfn.save.entities

import org.cqfn.save.teststatus.TestResultStatus
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property status
 * @property agentId
 * @property startTime
 * @property endTime
 * @property id
 */
@Entity
class TestStatus(
    @Enumerated(EnumType.STRING)
    var status: TestResultStatus,
    var agentId: String,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    @Id @GeneratedValue var id: Long? = null
)
