package com.saveourtool.save.test

import com.saveourtool.save.entities.DtoWithId
import kotlinx.datetime.LocalDateTime

/**
 * @property snapshotId ID of [TestsSourceSnapshotDto]
 * @property name human-readable version
 * @property creationTime time of creation this version
 * @property id ID of saved entity
 */
data class TestsSourceVersionDto(
    val snapshotId: Long,
    val name: String,
    val creationTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
