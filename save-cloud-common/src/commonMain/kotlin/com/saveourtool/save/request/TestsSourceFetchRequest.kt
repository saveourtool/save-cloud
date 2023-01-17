package com.saveourtool.save.request

import com.saveourtool.save.test.TestsSourceSnapshotDto
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import com.saveourtool.save.utils.getCurrentLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property source
 * @property mode
 * @property version
 * @property createdByUserId
 */
@Serializable
data class TestsSourceFetchRequest(
    val source: TestSuitesSourceDto,

    val mode: TestSuitesSourceFetchMode,
    val version: String,

    val createdByUserId: Long,
) {
    /**
     * @param commitId [TestsSourceSnapshotDto.commitId]
     * @param commitTime [TestsSourceSnapshotDto.commitTime]
     * @return [TestsSourceSnapshotDto] created by provided values and [TestsSourceFetchRequest]
     */
    fun createSnapshot(
        commitId: String,
        commitTime: LocalDateTime,
    ): TestsSourceSnapshotDto = TestsSourceSnapshotDto(
        sourceId = source.requiredId(),
        commitId = commitId,
        commitTime = commitTime,
    )

    /**
     * @param commitId [TestsSourceVersionInfo.commitId]
     * @param commitTime [TestsSourceVersionInfo.commitTime]
     * @return [TestsSourceVersionInfo] created by provided values and [TestsSourceFetchRequest]
     */
    fun createVersionInfo(
        commitId: String,
        commitTime: LocalDateTime,
    ): TestsSourceVersionInfo = TestsSourceVersionInfo(
        organizationName = source.organizationName,
        sourceName = source.name,
        commitId = commitId,
        commitTime = commitTime,
        version = version,
        creationTime = getCurrentLocalDateTime(),
    )
}
