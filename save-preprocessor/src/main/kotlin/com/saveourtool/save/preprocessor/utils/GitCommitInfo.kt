package com.saveourtool.save.preprocessor.utils

import java.time.Instant

/**
 * @property id hash commit
 * @property time commit time
 */
data class GitCommitInfo(
    val id: String,
    val time: Instant,
)
