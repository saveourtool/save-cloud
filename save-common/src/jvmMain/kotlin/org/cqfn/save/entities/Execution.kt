package org.cqfn.save.entities

import org.cqfn.save.execution.ExecutionStatus
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property startTime
 * @property endTime
 * @property status
 * @property testSuiteIds
 * @property offset
 * @property resourcesRootPath path to test resources, relative to shared volume mount point
 * @property executionLimit
 */
@Suppress("USE_DATA_CLASS")
@Entity
class Execution(

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var startTime: LocalDateTime,

    var endTime: LocalDateTime,

    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,

    var testSuiteIds: String,

    var resourcesRootPath: String,

    var offset: Int,

    var executionLimit: Int,

) : BaseEntity()
