package com.saveourtool.save.testsuite

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

typealias TestSuitesSourceVersionInfoList = List<TestSuitesSourceVersionInfo>

/**
 * @property organizationName
 * @property sourceName
 * @property version
 * @property creationTime
 * @property commitId
 * @property commitTime
 */
@Serializable
data class TestSuitesSourceVersionInfo(
    val organizationName: String,
    val sourceName: String,
    val version: String,
    val creationTime: LocalDateTime,
    val commitId: String,
    val commitTime: LocalDateTime,
)
