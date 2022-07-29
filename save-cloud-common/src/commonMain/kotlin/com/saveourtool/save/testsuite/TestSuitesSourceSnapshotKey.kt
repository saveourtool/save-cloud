package com.saveourtool.save.testsuite

import kotlinx.datetime.*
import kotlinx.serialization.Serializable

typealias TestSuitesSourceSnapshotKeyList = List<TestSuitesSourceSnapshotKey>

/**
 * @property organizationName
 * @property testSuitesSourceName
 * @property version
 * @property creationTimeInMills
 */
@Serializable
data class TestSuitesSourceSnapshotKey(
    val organizationName: String,
    val testSuitesSourceName: String,
    val version: String,
    val creationTimeInMills: Long,
) {
    constructor(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
        creationTime: LocalDateTime
    ) : this(
        organizationName,
        testSuitesSourceName,
        version,
        creationTimeToLong(creationTime),
    )

    constructor(testSuitesSourceDto: TestSuitesSourceDto, version: String, creationTimeInMills: Long) : this(
        testSuitesSourceDto.organizationName,
        testSuitesSourceDto.name,
        version,
        creationTimeInMills,
    )

    /**
     * @return [TestSuitesSourceSnapshotKey.creationTimeInMills] as [LocalDateTime] in [TimeZone.UTC]
     */
    fun convertAndGetCreationTime(): LocalDateTime = creationTimeFromLong(creationTimeInMills)

    /**
     * @param organizationName
     * @param testSuitesSourceName
     * @param version
     * @return true if object contains provided values
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun equalsTo(
        organizationName: String,
        testSuitesSourceName: String,
        version: String,
    ): Boolean = this.organizationName == organizationName && this.testSuitesSourceName == testSuitesSourceName && this.version == version

    companion object {
        private val creationTimeZoneId = TimeZone.UTC

        private fun creationTimeToLong(creationTime: LocalDateTime): Long = creationTime.toInstant(creationTimeZoneId)
            .toEpochMilliseconds()

        private fun creationTimeFromLong(creationTimeInMills: Long): LocalDateTime = Instant.fromEpochMilliseconds(creationTimeInMills)
            .toLocalDateTime(creationTimeZoneId)
    }
}
