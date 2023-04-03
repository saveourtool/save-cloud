package com.saveourtool.save.entities

import com.saveourtool.save.execution.ExecutionDto
import kotlinx.serialization.Serializable

/**
 * @property execution
 * @property file
 */
@Serializable
data class LnkExecutionFileDto(
    val execution: ExecutionDto,
    val file: FileDto,
)
