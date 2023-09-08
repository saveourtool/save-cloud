package com.saveourtool.save.entities.cosv

import com.saveourtool.save.entities.DtoWithId

/**
 * DTO for raw cosv file
 *
 * @property fileName
 * @property userName
 * @property organizationName
 * @property status
 * @property id
 */
data class RawCosvFileDto(
    val fileName: String,
    val userName: String,
    val organizationName: String,
    val status: RawCosvFileStatus,
    override val id: Long?,
): DtoWithId()
