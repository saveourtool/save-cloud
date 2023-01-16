package com.saveourtool.save.test

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

typealias TestsSourceVersionInfoList = List<TestsSourceVersionInfo>

/**
 * @property organizationName
 * @property sourceName
 * @property version
 * @property creationTime
 * @property commitId
 * @property commitTime
 */
@Serializable
data class TestsSourceVersionInfo(
    val organizationName: String,
    val sourceName: String,
    val version: String,
    val creationTime: LocalDateTime,
    val commitId: String,
    val commitTime: LocalDateTime,
)
