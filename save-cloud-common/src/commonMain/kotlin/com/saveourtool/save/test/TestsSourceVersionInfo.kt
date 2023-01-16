package com.saveourtool.save.test

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

typealias TestsSourceVersionInfoList = List<TestsSourceVersionInfo>

/**
 * @property snapshotInfo
 * @property version
 * @property creationTime
 */
@Serializable
data class TestsSourceVersionInfo(
    val snapshotInfo: TestsSourceSnapshotInfo,
    val version: String,
    val creationTime: LocalDateTime,
)
