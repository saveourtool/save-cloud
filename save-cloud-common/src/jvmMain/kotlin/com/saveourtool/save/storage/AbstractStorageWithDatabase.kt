package com.saveourtool.save.storage

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*
import kotlinx.datetime.Clock
import org.slf4j.Logger

import org.springframework.data.domain.Example
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import java.nio.ByteBuffer
import java.nio.file.Path
import java.time.Instant
import javax.annotation.PostConstruct
import kotlin.io.path.div

import kotlin.io.path.name

/**
 * Implementation of storage which stores keys in database
 *
 * @property storage some storage which uses [Long] ([DtoWithId.id]) as a key
 * @property backupStorageCreator creator for some storage which uses [Long] as a key, should be unique per each creation (to avoid duplication in backups)
 * @property repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabase<K : DtoWithId, E : BaseEntityWithDtoWithId<K>, R : BaseEntityRepository<E>>(
    private val storage: Storage<Long>,
    private val backupStorageCreator: () -> Storage<Long>,
    protected val repository: R,
) : Storage<K> {
    private val log: Logger = getLogger(this.javaClass)

    /**
     * Implementation using file-based storage
     *
     * @property rootDir root directory for storage
     * @property repository repository for [E] which is entity for [K]
     */
    constructor(
        rootDir: Path,
        repository: R,
    ) : this(defaultFileBasedStorage(rootDir), { defaultFileBasedStorage(rootDir / "backup-${Clock.System.now().epochSeconds}") }, repository)

    /**
     * Init method to back up unexpected ids which are detected in storage,but missed in database
     */
    @PostConstruct
    fun backupUnexpectedIds() {
        storage.list()
            .filterWhen { id ->
                blockingToMono {
                    repository.findById(id).isEmpty
                }
            }
            .collectList()
            .flatMapIterable { unexpectedIds ->
                if (unexpectedIds.isNotEmpty()) {
                    val backupStorage = backupStorageCreator()
                    log.warn {
                        "Found unexpected ids $unexpectedIds in storage ${this::class.simpleName}. Move them to backup storage..."
                    }
                    generateSequence { backupStorage }.take(unexpectedIds.size)
                        .toList()
                        .zip(unexpectedIds)
                } else {
                    emptyList()
                }
            }
            .flatMap { (backupStorage, id) ->
                backupStorage.upload(id, storage.download(id))
                    .then(storage.delete(id))
            }
            .subscribe()
    }

    override fun list(): Flux<K> = blockingToFlux {
        repository.findAll()
            .map { it.toDto() }
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

    override fun contentSize(key: K): Mono<Long> = getIdAsMono(key).flatMap { storage.contentSize(it) }

    override fun lastModified(key: K): Mono<Instant> = getIdAsMono(key).flatMap { storage.lastModified(it) }

    override fun delete(key: K): Mono<Boolean> = blockingToMono { findEntity(key) }
        .flatMap { entity ->
            storage.delete(entity.requiredId())
                .asyncEffectIf({ this }) {
                    doDelete(entity)
                }
        }
        .defaultIfEmpty(false)

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = doUpload(key, content).map(Pair<Any, Long>::second)

    /**
     * @param key a key for provided content
     * @param content
     * @return updated key [K]
     */
    fun uploadAndReturnUpdatedKey(key: K, content: Flux<ByteBuffer>): Mono<K> = doUpload(key, content).map(Pair<K, Any>::first)

    private fun doUpload(key: K, content: Flux<ByteBuffer>): Mono<Pair<K, Long>> = blockingToMono {
        repository.save(createNewEntityFromDto(key))
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), content)
                .flatMap { contentSize ->
                    blockingToMono { repository.save(entity.updateByContentSize(contentSize)) }
                        .map {
                            it.toDto() to contentSize
                        }
                }
                .onErrorResume { ex ->
                    doDelete(entity).then(Mono.error(ex))
                }
        }

    override fun download(key: K): Flux<ByteBuffer> = getIdAsMono(key).flatMapMany { storage.download(it) }

    private fun findEntity(dto: K): E? = dto.id
        ?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        }
        ?: findByDto(dto)

    private fun getIdAsMono(dto: K): Mono<Long> = blockingToMono { findEntity(dto)?.requiredId() }
        .switchIfEmptyToNotFound { "DTO $this is not saved: ID is not set and failed to find by default example" }

    private fun doDelete(entity: E): Mono<Unit> = blockingToMono {
        beforeDelete(entity)
        repository.delete(entity)
    }

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param dto
     * @return [E] entity found by [K] dto or null
     */
    protected open fun findByDto(dto: K): E? = repository.findOne(Example.of(createNewEntityFromDto(dto)))
        .orElseGet(null)

    /**
     * @param dto
     * @return a new [E] entity is created from provided [K] dto
     */
    abstract fun createNewEntityFromDto(dto: K): E

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

    companion object {
        private fun defaultFileBasedStorage(rootDir: Path): Storage<Long> = object : AbstractFileBasedStorage<Long>(rootDir, 1) {
            override fun buildKey(rootDir: Path, pathToContent: Path): Long = pathToContent.name.toLong()
            override fun buildPathToContent(rootDir: Path, key: Long): Path = rootDir.resolve(key.toString())
        }
    }
}
