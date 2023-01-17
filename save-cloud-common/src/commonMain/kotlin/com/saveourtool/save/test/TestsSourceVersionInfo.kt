package com.saveourtool.save.test

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

typealias TestsSourceVersionInfoList = List<TestsSourceVersionInfo>

/**
 * Info about tests snapshot with version provided by user
 *
 * @property snapshotInfo origin of this version
 * @property version human-readable version provided by user
 * @property creationTime time when this version is created
 */
@Serializable
data class TestsSourceVersionInfo(
    val snapshotInfo: TestsSourceSnapshotInfo,
    val version: String,
    val creationTime: LocalDateTime,
)
