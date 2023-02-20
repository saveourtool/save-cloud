package com.saveourtool.save.storage

import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.data.domain.Example
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.time.Instant

/**
 * Implementation of storage which stores keys in database
 *
 * @property storageProjectReactor some storage which uses [Long] ([BaseEntity.id]) as a key
 * @property repository repository for [E]
 */
abstract class AbstractStorageProjectReactorWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val storageProjectReactor: StorageProjectReactor<Long>,
    protected val repository: R,
) : StorageProjectReactor<K> {
    private val log: Logger = getLogger(this.javaClass)

    /**
     * @return a key [K] created from receiver entity [E]
     */
    protected abstract fun E.toKey(): K

    /**
     * @return an entity [E] created from receiver key [K]
     */
    protected abstract fun K.toEntity(): E

    override fun list(): Flux<K> = blockingToFlux {
        repository.findAll().map { it.toKey() }
    }

    override fun doesExist(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storageProjectReactor.doesExist(entity.requiredId())
                .filter { it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The key $key is presented in database, but missed in storage"
                }
        }
        .defaultIfEmpty(false)

    override fun contentLength(key: K): Mono<Long> = getIdAsMono(key).flatMap { storageProjectReactor.contentLength(it) }

    override fun lastModified(key: K): Mono<Instant> = getIdAsMono(key).flatMap { storageProjectReactor.lastModified(it) }

    override fun delete(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storageProjectReactor.delete(entity.requiredId())
                .asyncEffectIf({ this }) {
                    doDelete(entity)
                }
        }
        .defaultIfEmpty(false)

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<K> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storageProjectReactor.upload(entity.requiredId(), content)
                .map { entity.toKey() }
                .onErrorResume { ex ->
                    doDelete(entity).then(Mono.error(ex))
                }
        }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storageProjectReactor.upload(entity.requiredId(), contentLength, content)
                .map { entity.toKey() }
                .onErrorResume { ex ->
                    doDelete(entity).then(Mono.error(ex))
                }
        }

    override fun move(source: K, target: K): Mono<Boolean> = throw UnsupportedOperationException(
        "${AbstractStorageProjectReactorWithDatabase::class.simpleName} storage doesn't support moving"
    )

    override fun download(key: K): Flux<ByteBuffer> = getIdAsMono(key).flatMapMany { storageProjectReactor.download(it) }

    private fun getIdAsMono(key: K): Mono<Long> = blockingToMono { findEntity(key)?.requiredId() }
        .switchIfEmptyToNotFound { "Key $key is not saved: ID is not set and failed to find by default example" }

    private fun doDelete(entity: E): Mono<Unit> = blockingToMono {
        beforeDelete(entity)
        repository.delete(entity)
    }

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param key
     * @return [E] entity found by [K] key or null
     */
    protected abstract fun findEntity(key: K): E?

    /**
     * @receiver [E] entity which needs to be processed before deletion
     * @param entity
     */
    protected open fun beforeDelete(entity: E): Unit = Unit
}
