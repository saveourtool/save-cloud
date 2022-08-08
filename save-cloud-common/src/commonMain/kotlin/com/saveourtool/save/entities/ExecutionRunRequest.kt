package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.Sdk

data class ExecutionRunRequest(
    val project: ProjectDto,

    val testSuiteIds: List<Long>,
    val files: List<FileInfo>,

    val sdk: Sdk,
    val execCmd: String? = null,
    val batchSizeForAnalyzer: String? = null,
)