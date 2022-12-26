package com.saveourtool.save.entities

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.spring.entity.BaseEntityWithDto
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class File(
    @ManyToOne
    @JoinColumn(name = "project_id")
    val project: Project,

    val name: String,
    val uploadedTime: LocalDateTime,
    val sizeBytes: Long,
    val isExecutable: Boolean,
): BaseEntityWithDto<FileDto>() {
    override fun toDto(): FileDto = FileDto(
        projectCoordinates = project.toProjectCoordinates(),
        name = name,
        uploadedTime = uploadedTime.toKotlinLocalDateTime(),
        sizeBytes = sizeBytes,
        isExecutable = isExecutable,
    )
}

fun FileDto.toEntity(projectResolver: (ProjectCoordinates) -> Project): File = File(
    project = projectResolver(projectCoordinates),
    name = name,
    uploadedTime = uploadedTime.toJavaLocalDateTime(),
    sizeBytes = sizeBytes,
    isExecutable = isExecutable,
)