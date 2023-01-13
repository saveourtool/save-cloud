package com.saveourtool.save.entities

import com.saveourtool.save.domain.ProjectCoordinates
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates file belongs to the project with such coordinates
 * @property name name of file
 * @property uploadedTime a time when file was uploaded
 * @property sizeBytes size in bytes
 * @property isExecutable
 * @property id ID of saved entity or null
 */
@Serializable
data class FileDto(
    val projectCoordinates: ProjectCoordinates,
    val name: String,
    val uploadedTime: LocalDateTime,
    val sizeBytes: Long = -1L,
    val isExecutable: Boolean = false,
    override val id: Long? = null,
) : DtoWithId()
