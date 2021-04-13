package org.cqfn.save.entities

import org.cqfn.save.domain.TestResultStatus

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property test id of test
 * @property testSuiteExecutionId test suite execution id
 * @property agent agent id
 * @property project project id
 * @property status status of test execution
 * @property startTime start time
 * @property endTime finish time
 */
@Entity
@Table(name = "test_execution")
class TestExecution(

    @ManyToOne
    @JoinColumn(name = "test_id")
    var test: Test,

    @Column(name = "test_suite_execution_id")
    var testSuiteExecutionId: Long,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent,

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: TestResultStatus,

    @Column(name = "start_time")
    var startTime: LocalDateTime,

    @Column(name = "end_time")
    var endTime: LocalDateTime,

) : BaseEntity()
