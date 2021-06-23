package org.cqfn.save.entities

import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property startTime
 * @property endTime If the state is RUNNING we are not considering it, so it can never be null
 * @property status
 * @property testSuiteIds a list of test suite IDs, that should be executed under this Execution.
 * @property page
 * @property resourcesRootPath path to test resources, relative to shared volume mount point
 * @property batchSize Maximum number of returning tests per execution
 * @property type
 * @property version
 * @property passedTests
 * @property failedTests
 * @property skippedTests
 */
@Suppress("USE_DATA_CLASS", "LongParameterList")
@Entity
class Execution(

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var startTime: LocalDateTime,

    var endTime: LocalDateTime?,

    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,

    var testSuiteIds: String,

    var resourcesRootPath: String,

    var page: Int,

    var batchSize: Int,

    @Enumerated(EnumType.STRING)
    var type: ExecutionType,

    var version: String,

    var passedTests: Long,

    var failedTests: Long,

    var skippedTests: Long,

) : BaseEntity() {
    /**
     * @return Execution dto
     */
    fun toDto() = ExecutionDto(
        id!!,
        status,
        type,
        version,
        endTime?.toEpochSecond(ZoneOffset.UTC),
        passedTests,
        failedTests,
        skippedTests
    )
}
