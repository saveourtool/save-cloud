package com.saveourtool.save.entities

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultStatus

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property test test
 * @property execution during which this TestExecution should take place.
 * @property agent which will execute this TestExecution. Agent id is nullable because agent isn't created at the time when test execution is created
 * @property status status of test execution
 * @property startTime start time
 * @property endTime finish time
 * @property unmatched number of unmatched checks/validations in test (false negative results)
 * @property matched number of matched checks/validations in test (true positive results)
 * @property expected number of all checks/validations in test (unmatched + matched)
 * @property unexpected number of matched,but not expected checks/validations in test (false positive results)
 */
@Entity
@Suppress("LongParameterList")
class TestExecution(

    @ManyToOne
    @JoinColumn(name = "test_id")
    var test: Test,

    @ManyToOne
    @JoinColumn(name = "execution_id")
    var execution: Execution,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent?,

    @Enumerated(EnumType.STRING)
    var status: TestResultStatus,

    var startTime: LocalDateTime?,

    var endTime: LocalDateTime?,

    var unmatched: Long?,

    var matched: Long?,

    var expected: Long?,

    var unexpected: Long?,

) : BaseEntity() {
    /**
     * Converts `this` to [TestExecutionDto]
     *
     * @return a new [TestExecutionDto]
     */
    fun toDto() = TestExecutionDto(
        test.filePath,
        test.pluginName,
        agent?.containerId,
        status,
        startTime?.toEpochSecond(ZoneOffset.UTC),
        endTime?.toEpochSecond(ZoneOffset.UTC),
        test.testSuite.name,
        test.testSuite.tagsAsList() ,
        unmatched,
        matched,
        expected,
        unexpected,
        null,
        execution.id,
    )
}
