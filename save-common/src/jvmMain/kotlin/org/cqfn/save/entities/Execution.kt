package org.cqfn.save.entities

import org.cqfn.save.execution.ExecutionStatus
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * @property project
 * @property startTime
 * @property endTime
 * @property status
 * @property testSuiteIds
 * @property resourcesRootPath path to test resources, relative to shared volume mount point
 */
@Suppress("USE_DATA_CLASS")
@Entity
@Table(name = "execution")
class Execution(

    @JoinColumn(name = "id")
    @ManyToOne
    @Column(name = "project_id")
    var project: Project,

    @Column(name = "start_time")
    var startTime: LocalDateTime,

    @Column(name = "end_time")
    var endTime: LocalDateTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    var status: ExecutionStatus,

    @Column(name = "test_suite_ids")
    var testSuiteIds: String,

    @Column(name = "resources_root_path")
    var resourcesRootPath: String,

) : BaseEntity()
