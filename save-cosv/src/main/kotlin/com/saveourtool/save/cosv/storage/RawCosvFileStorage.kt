package com.saveourtool.save.cosv.storage

import com.saveourtool.save.cosv.repository.RawCosvFileRepository
import com.saveourtool.save.entities.cosv.RawCosvFile
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

/**
 * S3 storage for [RawCosvFile]
 */
@Component
class RawCosvFileStorage(
    s3Operations: S3Operations,
    s3KeyManager: RawCosvFileS3KeyManager,
    repository: RawCosvFileRepository,
) : ReactiveStorageWithDatabase<RawCosvFileDto, RawCosvFile, RawCosvFileRepository, RawCosvFileS3KeyManager>(
    s3Operations = s3Operations,
    s3KeyManager = s3KeyManager,
    repository = repository,
) {
    /**
     * @param id
     * @return content of raw COSV file
     */
    fun downloadById(
        id: Long,
    ): Flux<ByteBuffer> = blockingToMono {
        s3KeyManager.findKeyById(id)
    }
        .orNotFound { "Not found raw COSV file id=$id" }
        .flatMapMany { download(it) }

    /**
     * @param id
     * @return result of deletion
     */
    fun deleteById(
        id: Long,
    ): Mono<Boolean> = blockingToMono {
        s3KeyManager.findKeyById(id)
    }
        .orNotFound { "Not found raw COSV file id=$id" }
        .flatMap { delete(it) }
}
