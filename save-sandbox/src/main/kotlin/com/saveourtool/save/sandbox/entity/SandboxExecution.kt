package com.saveourtool.save.sandbox.entity

import com.saveourtool.save.domain.toSdk
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.request.RunExecutionRequest
import com.saveourtool.save.spring.entity.BaseEntity
import java.net.URL

import java.time.LocalDateTime
import javax.persistence.*

/**
 * @property startTime
 * @property endTime
 * @property status
 * @property sdk
 * @property saveCliVersion
 * @property userId
 * @property initialized
 * @property failReason
 */
@Entity
@Table(name = "execution")
@Suppress("LongParameterList")
class SandboxExecution(
    var startTime: LocalDateTime,
    var endTime: LocalDateTime?,
    @Enumerated(EnumType.STRING)
    var status: ExecutionStatus,
    var sdk: String,
    var saveCliVersion: String,
    @Column(name = "user_id")
    var userId: Long,
    var initialized: Boolean,
    var failReason: String?,
) : BaseEntity() {
    /**
     * @param saveAgentUrl an url to download save-agent
     * @return [RunExecutionRequest] created from current entity
     */
    fun toRunRequest(
        saveAgentUrl: URL,
    ): RunExecutionRequest {
        require(status == ExecutionStatus.PENDING) {
            "${RunExecutionRequest::class.simpleName} can be created only for ${Execution::class.simpleName} with status = ${ExecutionStatus.PENDING}"
        }
        return RunExecutionRequest(
            executionId = requiredId(),
            sdk = sdk.toSdk(),
            saveAgentUrl = saveAgentUrl.toString(),
        )
    }
}
