package org.cqfn.save.entities

import org.cqfn.save.domain.TestResultStatus

import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property id id of test
 * @property status of test
 * @property date date of result
 */
@Entity
class TestExecution(
    @Id @GeneratedValue var id: Long,
    var testId: Long,
    var testSuiteExecutionId: Long,
    var agentId: Long,
    var projectId: Long,
    @Enumerated(EnumType.STRING)
    var status: TestResultStatus,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
)
