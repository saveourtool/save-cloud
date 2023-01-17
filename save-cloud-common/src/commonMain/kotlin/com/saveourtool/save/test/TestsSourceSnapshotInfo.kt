package com.saveourtool.save.test

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * A technical info about tests snapshot
 *
 * @property organizationName
 * @property sourceName
 * @property commitId commit hash, unique ID of tests snapshot
 * @property commitTime commit time according to git history
 */
@Serializable
data class TestsSourceSnapshotInfo(
    val organizationName: String,
    val sourceName: String,
    val commitId: String,
    val commitTime: LocalDateTime,
)
