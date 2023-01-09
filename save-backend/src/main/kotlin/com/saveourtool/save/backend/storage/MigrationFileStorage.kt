package com.saveourtool.save.backend.storage

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.storage.AbstractMigrationStorage
import com.saveourtool.save.utils.millisToInstant

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

import javax.annotation.PostConstruct

import kotlinx.datetime.*

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class MigrationFileStorage(
    oldFileStorage: FileStorage,
    newFileStorage: NewFileStorage,
) : AbstractMigrationStorage<FileKey, FileDto>(oldFileStorage, newFileStorage) {
    /**
     * A temporary init method which copies file from one storage to another
     */
    @PostConstruct
    fun init() {
        super.migration()
    }

    override fun FileDto.toOldKey(): FileKey = FileKey(
        projectCoordinates = projectCoordinates,
        name = name,
        uploadedMillis = uploadedTime.toInstant(TimeZone.UTC).toEpochMilliseconds()
    )

    override fun FileKey.toNewKey(): FileDto = FileDto(
        projectCoordinates = this.projectCoordinates,
        name = this.name,
        uploadedTime = this.uploadedMillis.millisToInstant().toLocalDateTime(TimeZone.UTC),
    )

    /**
     * @param projectCoordinates
     * @return a list of [FileInfo]'s
     */
    fun getFileInfoList(
        projectCoordinates: ProjectCoordinates,
    ): Flux<FileInfo> = list(projectCoordinates)
        .flatMap { fileKey ->
            contentSize(fileKey).map {
                FileInfo(
                    fileKey,
                    it,
                )
            }
        }

    /**
     * @param projectCoordinates
     * @return list of keys in storage by [projectCoordinates]
     */
    fun list(projectCoordinates: ProjectCoordinates): Flux<FileKey> = list()
        .filter { it.projectCoordinates == projectCoordinates }
}
