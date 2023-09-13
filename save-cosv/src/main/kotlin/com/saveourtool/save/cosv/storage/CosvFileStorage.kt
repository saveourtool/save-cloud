package com.saveourtool.save.cosv.storage

import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageProjectReactor
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

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
    override fun doInit(underlying: DefaultStorageProjectReactor<CosvFile>): Mono<Unit> {
        // do nothing till migration is done
        return Mono.just(Unit)
    }
}
