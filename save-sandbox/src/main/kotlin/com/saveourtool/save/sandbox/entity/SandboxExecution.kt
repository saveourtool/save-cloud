package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.entities.BaseEntity
import com.saveourtool.save.entities.User
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.utils.orConflict
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "execution")
class SandboxExecution(
    var startTime: LocalDateTime,
    var endTime: LocalDateTime?,
    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,
    var sdk: String,
    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,
    var failReason: String?,
) : BaseEntity() {
    /**
     * @return [User.name]
     */
    fun getUserName(): String = user
        .name
        .orConflict {
            "All users should have name"
        }
}
