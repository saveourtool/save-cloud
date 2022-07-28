package com.saveourtool.save.testsuite

import kotlinx.datetime.*
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
    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @param creationTimeInMills
     */
    constructor(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
        creationTimeInMills: Long
    ) : this(
        organizationName,
        testSuitesSourceName,
        version,
        Instant.fromEpochMilliseconds(creationTimeInMills).toLocalDateTime(creationTimeZoneId),
    )

    /**
     * @param testSuitesSourceDto
     * @param version
     * @param creationTimeInMills
     */
    constructor(testSuitesSourceDto: TestSuitesSourceDto, version: String, creationTimeInMills: Long) : this(
        testSuitesSourceDto.organizationName,
        testSuitesSourceDto.name,
        version,
        creationTimeInMills,
    )

    /**
     * @return [TestSuitesSourceSnapshotKey.creationTime] as milliseconds from epoch
     */
    fun getCreationTimeInMills(): Long = creationTime.toInstant(TimeZone.UTC)
        .toEpochMilliseconds()


    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return true if object contains provided values
     */
    fun equalsTo(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Boolean = this.organizationName == organizationName && this.testSuitesSourceName == testSuitesSourceName && this.version == version

    companion object {
        private val creationTimeZoneId = TimeZone.UTC
    }
}
