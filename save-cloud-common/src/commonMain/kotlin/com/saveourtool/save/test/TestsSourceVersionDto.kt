package com.saveourtool.save.test

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import kotlinx.datetime.LocalDateTime

/**
 * @property snapshot [TestsSourceSnapshotDto]// TODO: should be replaced to snapshotId after removing [com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey]
 * @property name human-readable version
 * @property type type of fetch mode
 * @property createdByUserId ID of [com.saveourtool.save.info.UserInfo] created this version
 * @property creationTime time of creation this version
 * @property id ID of saved entity
 */
data class TestsSourceVersionDto(
    val snapshot: TestsSourceSnapshotDto,
    val name: String,
    val type: TestSuitesSourceFetchMode,
    val createdByUserId: Long,
    val creationTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
