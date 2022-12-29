package com.saveourtool.save.backend.storage

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.entities.FileDto
import com.saveourtool.save.entities.toFileDto
import com.saveourtool.save.storage.Storage
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.millisToInstant
import kotlinx.datetime.*
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    private val newFileStorage: NewFileStorage,
) : Storage<FileKey> {
    /**
     * A temporary init method which copies file from one storage to another
     */
    @PostConstruct
    fun migration() {
        list()
            .map { fileKey ->
                fileKey to fileKey.toFileDto()
            }
            .flatMap { (fileKey, fileDto) ->
                newFileStorage.upload(fileDto, download(fileKey))
                    .map {
                        log.info {
                            "Copied $fileKey to new storage with key $fileDto"
                        }
                    }
                    .flatMap {
                        delete(fileKey)
                    }
            }
            .subscribe()
    }

    /**
     * @param projectCoordinates
     * @return list of keys in storage by [projectCoordinates]
     */
    fun list(projectCoordinates: ProjectCoordinates): Flux<FileKey> = list()
        .filter { it.projectCoordinates == projectCoordinates }

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

    override fun list(): Flux<FileKey> {
        return newFileStorage.list()
            .map { fileDto ->
                FileKey(
                    projectCoordinates = fileDto.projectCoordinates,
                    name = fileDto.name,
                    uploadedMillis = fileDto.uploadedTime.toInstant(TimeZone.UTC).toEpochMilliseconds()
                )
            }
    }

    override fun download(key: FileKey): Flux<ByteBuffer> = newFileStorage.download(key.toFileDto())

    override fun upload(key: FileKey, content: Flux<ByteBuffer>): Mono<Long> = newFileStorage.upload(key.toFileDto(), content)

    override fun delete(key: FileKey): Mono<Boolean> = newFileStorage.delete(key.toFileDto())

    override fun lastModified(key: FileKey): Mono<Instant> = newFileStorage.lastModified(key.toFileDto())

    override fun contentSize(key: FileKey): Mono<Long> = newFileStorage.contentSize(key.toFileDto())

    override fun doesExist(key: FileKey): Mono<Boolean> = newFileStorage.doesExist(key.toFileDto())

    companion object {
        private val log: Logger = getLogger<FileStorage>()
    }
}
