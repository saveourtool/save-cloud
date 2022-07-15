package com.saveourtool.save.testsuite

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @param organizationName
 * @param testSuitesSourceName
 * @param version
 * @param creationTime
 */
@Serializable
data class TestSuitesSourceSnapshotKey(
    val organizationName: String,
    val testSuitesSourceName: String,
    val version: String,
    val creationTime: LocalDateTime,
) {
    constructor(testSuitesSourceDto: TestSuitesSourceDto, version: String, creationTime: LocalDateTime) : this(
        testSuitesSourceDto.organization.name,
        testSuitesSourceDto.name,
        version,
        creationTime,
    )
}
