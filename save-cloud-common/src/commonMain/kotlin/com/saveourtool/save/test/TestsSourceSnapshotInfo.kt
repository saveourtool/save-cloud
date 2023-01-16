package com.saveourtool.save.test

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property organizationName
 * @property sourceName
 * @property commitId
 * @property commitTime
 */
@Serializable
data class TestsSourceSnapshotInfo(
    val organizationName: String,
    val sourceName: String,
    val commitId: String,
    val commitTime: LocalDateTime,
)
