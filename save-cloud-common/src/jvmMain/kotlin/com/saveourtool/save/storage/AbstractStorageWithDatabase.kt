package com.saveourtool.save.storage

import com.saveourtool.save.s3.S3OperationsProjectReactor
import com.saveourtool.save.spring.entity.BaseEntity
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*

import org.slf4j.Logger
import org.springframework.data.domain.Example
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Implementation of storage which stores keys in database and S3 as underlying storage
 *
 * @param s3Operations interface to operate with S3 storage
 * @param prefix a common prefix for all keys in S3 storage for this storage
 * @property repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabase<K : Any, E : BaseEntity, R : BaseEntityRepository<E>>(
    private val s3Operations: S3OperationsProjectReactor,
    private val prefix: String,
    protected val repository: R,
) : Storage<K> {
    private val log: Logger = getLogger(this::class)
    private val storage: UnderlyingStorageWithBackup = UnderlyingStorageWithBackup()

    /**
     * Method to call init method in underlying storage
     */
    @PostConstruct
    fun init() {
        storage.init()
    }

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
            storage.doesExist(entity.requiredId())
                .filter { it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The key $key is presented in database, but missed in storage"
                }
        }
        .defaultIfEmpty(false)

    override fun contentLength(key: K): Mono<Long> = getIdAsMono(key).flatMap { storage.contentLength(it) }

    override fun lastModified(key: K): Mono<Instant> = getIdAsMono(key).flatMap { storage.lastModified(it) }

    override fun delete(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storage.delete(entity.requiredId())
                .asyncEffectIf({ this }) {
                    doDeleteAsMono(entity)
                }
        }
        .defaultIfEmpty(false)

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = doUpload(key, content).map(Pair<Any, Long>::second)

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = uploadAndReturnUpdatedKey(key, contentLength, content).thenReturn(Unit)

    /**
     * @param key a key for provided content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, content: Flux<ByteBuffer>): Mono<K> = doUpload(key, content).map(Pair<K, Any>::first)

    private fun doUpload(key: K, content: Flux<ByteBuffer>): Mono<Pair<K, Long>> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), content)
                .flatMap { contentSize ->
                    blockingToMono { repository.save(entity.updateByContentSize(contentSize)) }
                        .map {
                            it.toKey() to contentSize
                        }
                }
                .onErrorResume { ex ->
                    doDeleteAsMono(entity).then(Mono.error(ex))
                }
        }

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return updated key [E]
     */
    open fun uploadAndReturnUpdatedKey(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<K> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), contentLength, content)
                .map { entity.toKey() }
                .onErrorResume { ex ->
                    doDeleteAsMono(entity).then(Mono.error(ex))
                }
        }

    /**
     * @throws Exception
     */
    override suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>) {
        uploadAndReturnUpdatedKey(key, contentLength, content)
    }

    /**
     * @param key a key for provided content
     * @param contentLength a content length of content
     * @param content
     * @return updated key [E]
     * @throws Exception
     */
    open suspend fun uploadAndReturnUpdatedKey(key: K, contentLength: Long, content: Flow<ByteBuffer>): K {
        val entity = withContext(Dispatchers.IO) {
            repository.save(key.toEntity())
        }
        try {
            storage.upload(entity.requiredId(), contentLength, content)
        } catch (ex: Exception) {
            withContext(Dispatchers.IO) {
                doDelete(entity)
            }
            throw ex
        }
        return entity.toKey()
    }

    override fun move(source: K, target: K): Mono<Boolean> = throw UnsupportedOperationException("${AbstractStorageWithDatabase::class.simpleName} storage doesn't support moving")

    override fun download(key: K): Flux<ByteBuffer> = getIdAsMono(key).flatMapMany { storage.download(it) }

    override fun generateUrlToDownload(key: K): URL = getId(key).let { storage.generateUrlToDownload(it) }

    override fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders = throw UnsupportedOperationException(
        "${AbstractStorageWithDatabase::class.simpleName} storage doesn't support pre-signed url to upload"
    )

    private fun getIdAsMono(key: K): Mono<Long> = blockingToMono { findEntity(key)?.requiredId() }
        .switchIfEmptyToNotFound { "Key $key is not saved: ID is not set and failed to find by default example" }

    private fun getId(key: K): Long = findEntity(key)?.requiredId().orNotFound { "Key $key is not saved: ID is not set and failed to find by default example" }

    private fun doDelete(entity: E) {
        beforeDelete(entity)
        repository.delete(entity)
    }

    private fun doDeleteAsMono(entity: E): Mono<Unit> = blockingToMono {
        doDelete(entity)
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

    /**
     * @receiver [E] entity which needs to be updated by [sizeBytes]
     * @param sizeBytes
     * @return updated [E] entity
     */
    protected open fun E.updateByContentSize(sizeBytes: Long): E = this

    private inner class UnderlyingStorageWithBackup : StorageWrapperWithInit<Long>() {
        override val log: Logger = this@AbstractStorageWithDatabase.log
        override val storageName: String = this@AbstractStorageWithDatabase::class.simpleName ?: this@AbstractStorageWithDatabase::class.java.simpleName
        override fun createUnderlyingStorage(): Storage<Long> = UnderlyingStorage(prefix)

        /**
         * Init method to back up unexpected ids which are detected in storage,but missed in database
         *
         * @return [Mono] without body
         */
        override fun doInitAsync(underlying: Storage<Long>): Mono<Unit> = underlying.detectAsyncUnexpectedIds(repository)
            .collectList()
            .filter { it.isNotEmpty() }
            .flatMapIterable { unexpectedIds ->
                val backupStorage = UnderlyingStorage(prefix.removeSuffix(PATH_DELIMITER) + "-backup-${Clock.System.now().epochSeconds}")
                log.warn {
                    "Found unexpected ids $unexpectedIds in storage $storageName. Move them to backup storage..."
                }
                generateSequence { backupStorage }.take(unexpectedIds.size)
                    .toList()
                    .zip(unexpectedIds)
            }
            .flatMap { (backupStorage, id) ->
                underlying.contentLength(id)
                    .flatMap { contentLength ->
                        backupStorage.upload(id, contentLength, underlying.download(id))
                    }
                    .then(underlying.delete(id))
            }
            .collectList()
            .map {
                log.info {
                    "Moved unexpected ids in $storageName to backup storage"
                }
            }
    }

    private open inner class UnderlyingStorage(underlyingPrefix: String) : AbstractS3Storage<Long>(s3Operations, underlyingPrefix) {
        override fun buildKey(s3KeySuffix: String): Long = s3KeySuffix.toLong()
        override fun buildS3KeySuffix(key: Long): String = key.toString()
    }
}
