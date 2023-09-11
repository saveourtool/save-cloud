package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.DtoWithId
import kotlinx.serialization.Serializable

/**
 * DTO for raw cosv file
 *
 * @property fileName
 * @property userName
 * @property organizationName
 * @property status
 * @property id
 */
@Serializable
data class RawCosvFileDto(
    val fileName: String,
    val userName: String,
    val organizationName: String,
    val status: RawCosvFileStatus = RawCosvFileStatus.UPLOADED,
    val errorMessage: String? = null,
    override val id: Long? = null,
) : DtoWithId()
