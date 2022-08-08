package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.Sdk

data class ExecutionRunRequestByContest(
    val project: ProjectDto,

    val contestId: Long,
    val files: List<FileInfo>,

    val sdk: Sdk,
    val execCmd: String? = null,
    val batchSizeForAnalyzer: String? = null,
)