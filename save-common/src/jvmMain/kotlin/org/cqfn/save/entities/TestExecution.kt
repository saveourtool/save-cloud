package org.cqfn.save.entities

import org.cqfn.save.domain.TestResultStatus

import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property id id of test execution
 * @property testId id of test
 * @property testSuiteExecutionId test suite execution id
 * @property agentId agent id
 * @property projectId project id
 * @property status status of test execution
 * @property startTime start time
 * @property endTime finish time
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
