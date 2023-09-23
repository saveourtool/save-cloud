package com.saveourtool.save.cosv.storage

import com.saveourtool.save.entities.cosv.CosvFile
import com.saveourtool.save.s3.S3Operations
import com.saveourtool.save.storage.DefaultStorageProjectReactor
import com.saveourtool.save.storage.ReactiveStorageWithDatabase
import com.saveourtool.save.storage.deleteUnexpectedKeys
import com.saveourtool.save.utils.blockingToFlux
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Storage for COSV files.
 *
 * For now, we store COSV files in S3 with `id` as a key.
 * We can migrate to NoSql database in the future.
 */
@Service
class CosvFileStorage(
    private val s3Operations: S3Operations,
    s3KeyManager: CosvFileS3KeyManager,
) : ReactiveStorageWithDatabase<CosvFile, CosvFile, CosvFileS3KeyManager>(
    s3Operations,
    s3KeyManager,
) {
    /**
     * Init method to remove deleted (unexpected) ids which are detected in storage, but missed in database
     */
    override fun doInit(underlying: DefaultStorageProjectReactor<CosvFile>): Mono<Unit> = Mono.fromFuture {
        s3Operations.deleteUnexpectedKeys(
            storageName = "${this::class.simpleName}",
            s3KeyManager = s3KeyManager,
        )
    }.publishOn(s3Operations.scheduler)

    /**
     * @param identifier
     * @return [Flux] with all [CosvFile] found by [identifier]
     */
    fun listByIdentifier(identifier: String): Flux<CosvFile> = blockingToFlux {
        s3KeyManager.findAllByIdentifier(identifier)
    }
}
