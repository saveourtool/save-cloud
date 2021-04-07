package org.cqfn.save.entities

import org.cqfn.save.execution.ExecutionStatus
import java.time.LocalDateTime
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property projectId
 * @property startTime
 * @property endTime
 * @property status
 * @property testSuiteIds
 * @property resourcesRootPath path to test resources, relative to shared volume mount point
 */
@Suppress("USE_DATA_CLASS")
@Entity
class Execution(
    var projectId: Long,
    var startTime: LocalDateTime,
    var endTime: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,
    var testSuiteIds: String,
    var resourcesRootPath: String,
) {
    /**
     * id
     */
    @Id @GeneratedValue var id: Long? = null
}
