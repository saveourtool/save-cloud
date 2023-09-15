package com.saveourtool.save.cosv.storage

import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.utils.blockingToMono
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

/**
 * Storage for COSV files.
 *
 * For now, we store COSV files in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class CosvFileStorage(
    s3Operations: S3Operations,
    s3KeyManager: CosvFileS3KeyManager,
) : ReactiveStorageWithDatabase<CosvFile, CosvFile, CosvFileS3KeyManager>(
    s3Operations,
    s3KeyManager
) {
    /**
     * @param identifier
     * @return content of latest COSV file found by [identifier]
     */
    fun downloadLatest(identifier: String): Flux<ByteBuffer> = blockingToMono { s3KeyManager.findLatest(identifier) }
        .flatMapMany { download(it) }
}
