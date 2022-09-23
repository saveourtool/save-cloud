package com.saveourtool.save.request

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.execution.ExecutionStatus
import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates project coordinates for evaluated tool
 * @property executionId ID of [com.saveourtool.save.entities.Execution]
 * @property sdk
 * @property status
 */
@Serializable
data class RunExecutionRequest(
    val projectCoordinates: ProjectCoordinates,
    val executionId: Long,
    val sdk: Sdk,
    val status: ExecutionStatus = ExecutionStatus.PENDING
)
