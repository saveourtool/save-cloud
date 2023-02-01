package com.saveourtool.save.backend.storage

import com.saveourtool.save.storage.AbstractMigrationStorage
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer

/**
 * Migration storage for [AvatarS3Storage]
 */
@Service
class MigrationAvatarStorage(
    oldStorage: AvatarStorage,
    newStorage: AvatarS3Storage,
) : AbstractMigrationStorage<AvatarKey, AvatarKey>(oldStorage, newStorage) {
    override fun AvatarKey.toNewKey(): AvatarKey = this
    override fun AvatarKey.toOldKey(): AvatarKey = this

    /**
     * @param key
     * @param content
     * @return `Mono` with file size
     */
    fun upsert(key: AvatarKey, content: Flux<ByteBuffer>): Mono<Long> = list()
        .filter { it == key }
        .singleOrEmpty()
        .flatMap { delete(it) }
        .switchIfEmpty(Mono.just(true))
        .flatMap { upload(key, content) }
}
