package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.DtoWithId
import kotlinx.datetime.LocalDateTime

/**
 * @property snapshotId ID of [TestSuitesSourceSnapshotDto]
 * @property name human-readable version
 * @property type type of fetch mode
 * @property createdByUserId ID of [UserDto] created this version
 * @property creationTime time of creation this version
 * @property id ID of saved entity
 */
data class TestSuitesSourceVersionDto(
    val snapshotId: Long,
    val name: String,
    val type: TestSuitesSourceFetchMode,
    val createdByUserId: Long,
    val creationTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
