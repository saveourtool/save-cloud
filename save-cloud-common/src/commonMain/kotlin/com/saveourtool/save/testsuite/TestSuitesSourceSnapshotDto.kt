package com.saveourtool.save.testsuite

import com.saveourtool.save.entities.DtoWithId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property sourceId ID of [TestSuitesSourceDto]
 * @property commitId hash-code
 * @property commitTime time of commit
 * @property id ID of saved entity
 */
@Serializable
data class TestSuitesSourceSnapshotDto(
    val sourceId: Long,
    val commitId: String,
    val commitTime: LocalDateTime,
    override val id: Long? = null,
) : DtoWithId()
