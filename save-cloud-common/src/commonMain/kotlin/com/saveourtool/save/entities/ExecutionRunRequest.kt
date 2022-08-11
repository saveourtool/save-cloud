package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import kotlinx.serialization.Serializable

@Serializable
data class ExecutionRunRequest(
    val projectCoordinates: ProjectCoordinates,

    val testSuiteIds: List<Long>,
    val files: List<FileKey>,

    val sdk: Sdk,
    val execCmd: String? = null,
    val batchSizeForAnalyzer: String? = null,
)