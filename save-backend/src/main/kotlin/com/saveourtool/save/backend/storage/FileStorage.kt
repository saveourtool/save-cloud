package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.repository.FileRepository
import com.saveourtool.save.entities.*
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.utils.*

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.net.URL

/**
 * Storage for evaluated tools are loaded by users
 */
@Service
class FileStorage(
    s3Operations: S3Operations,
    fileRepository: FileRepository,
    s3KeyManager: FileS3KeyManager,
) : ReactiveStorageWithDatabase<FileDto, File, FileRepository, FileS3KeyManager>(
    s3Operations,
    s3KeyManager,
    fileRepository
) {
    /**
     * @param project
     * @return all [FileDto]s which provided [Project] does contain
     */
    fun listByProject(
        project: Project,
    ): Flux<FileDto> = blockingToFlux {
        s3KeyManager.listByProject(project)
    }

    /**
     * @param fileId
     * @return [FileDto] for [File] with provided [fileId]
     */
    fun getFileById(
        fileId: Long,
    ): Mono<FileDto> = blockingToMono {
        s3KeyManager.findFileById(fileId)
    }
        .switchIfEmptyToNotFound { "Not found a file by id $fileId" }
}
