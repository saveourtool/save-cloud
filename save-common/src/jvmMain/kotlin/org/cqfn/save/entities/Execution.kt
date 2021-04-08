@file:UseSerializers(LocalDateTimeSerializer::class)

package org.cqfn.save.entities

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.utils.LocalDateTimeSerializer
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

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
