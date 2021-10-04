package org.cqfn.save.entities

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultStatus

import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property test id of test
 * @property executionId id of the Execution during which this TestExecution should take place. todo: change to Execution
 * @property agent Id of the agent which will execute this TestExecution. Agent id is nullable because agent isn't created at the time when test execution is created
 * @property status status of test execution
 * @property startTime start time
 * @property endTime finish time
 */
@Entity
class TestExecution(

    @ManyToOne
    @JoinColumn(name = "test_id")
    var test: Test,

    var executionId: Long,

    @ManyToOne
    @JoinColumn(name = "agent_id")
    var agent: Agent?,

    @Enumerated(EnumType.STRING)
    var status: TestResultStatus,

    var startTime: LocalDateTime?,

    var endTime: LocalDateTime?,

) : BaseEntity() {
    /**
     * Converts `this` to [TestExecutionDto]
     *
     * @return a new [TestExecutionDto]
     */
    @Suppress("UnsafeCallOnNullableType")
    fun toDto() = TestExecutionDto(
        test.filePath,
        test.pluginName,
        agent?.containerId,
        status,
        startTime?.toEpochSecond(ZoneOffset.UTC),
        endTime?.toEpochSecond(ZoneOffset.UTC),
        test.testSuite.name,
        test.tags!!.split(";").filter { it.isNotBlank() },
    )
}
