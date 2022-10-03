package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.BaseEntity
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.User
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.request.RunExecutionRequest
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
    var user: SandboxUser,
    var failReason: String?,
) : BaseEntity() {
    /**
     * @return [User.name]
     */
    fun getUserName(): String = user
        .name

    /**
     * @return [RunExecutionRequest] created from current entity
     */
    fun toRunRequest(): RunExecutionRequest {
        require(status == ExecutionStatus.PENDING) {
            "${RunExecutionRequest::class.simpleName} can be created only for ${Execution::class.simpleName} with status = ${ExecutionStatus.PENDING}"
        }
        return RunExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = getUserName(),
                projectName = "sandbox",
            ),
            executionId = requiredId(),
            sdk = sdk.toSdk()
        )
    }
}
