package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.DtoWithId
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * DTO for raw cosv file
 *
 * @property fileName
 * @property userName
 * @property organizationName
 * @property status
 * @property statusMessage
 * @property updateDate
 * @property id
 */
@Serializable
data class RawCosvFileDto(
    val fileName: String,
    val userName: String,
    val organizationName: String,
    val status: RawCosvFileStatus = RawCosvFileStatus.UPLOADED,
    val statusMessage: String? = null,
    val updateDate: LocalDateTime? = null,
    override val id: Long? = null,
) : DtoWithId()
