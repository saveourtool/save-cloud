package com.saveourtool.save.preprocessor.utils

import java.time.Instant

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime

/**
 * @property id hash commit
 * @property time commit time
 */
data class GitCommitInfo(
    val id: String,
    val time: LocalDateTime,
) {
    constructor(
        id: String,
        time: Instant,
    ) : this(id, time.toKotlinInstant().toLocalDateTime(TimeZone.UTC))
}
