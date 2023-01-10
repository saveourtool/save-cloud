package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.millisToInstant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
) : DtoWithId() {
    /**
     * @return [FileKey] is built from this [FileDto]
     */
    fun toFileKey(): FileKey = FileKey(
        projectCoordinates = this.projectCoordinates,
        name = this.name,
        uploadedMillis = this.uploadedTime.toInstant(TimeZone.UTC).toEpochMilliseconds(),
    )
}

/**
 * @receiver [FileKey]
 * @return [FileDto] is built from receiver
 */
fun FileKey.toFileDto(): FileDto = FileDto(
    projectCoordinates = this.projectCoordinates,
    name = this.name,
    uploadedTime = this.uploadedMillis.millisToInstant().toLocalDateTime(TimeZone.UTC),
)
