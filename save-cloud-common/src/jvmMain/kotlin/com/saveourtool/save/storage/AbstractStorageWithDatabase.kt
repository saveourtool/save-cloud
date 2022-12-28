package com.saveourtool.save.storage

import com.saveourtool.save.entities.DtoWithId
import com.saveourtool.save.spring.entity.BaseEntityWithDtoWithId
import com.saveourtool.save.spring.repository.BaseEntityRepository
import com.saveourtool.save.utils.*
import org.springframework.data.domain.Example
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Instant
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.nio.file.Path
import kotlin.io.path.name

/**
 * Implementation of storage which stores keys in database
 *
 * @property storage some storage which uses [Long] ([DtoWithId.id]) as a key
 * @property repository repository for [E] which is entity for [K]
 */
abstract class AbstractStorageWithDatabase<K : DtoWithId, E : BaseEntityWithDtoWithId<K>>(
    private val storage: Storage<Long>,
    private val repository: BaseEntityRepository<E>,
) : Storage<K> {
    /**
     * Implementation using file-based storage
     *
     * @property rootDir root directory for storage
     * @property repository repository for [E] which is entity for [K]
     */
    constructor(
        rootDir: Path,
        repository: BaseEntityRepository<E>,
    ) : this(defaultFileBasedStorage(rootDir), repository)

    override fun list(): Flux<K> = blockingToFlux {
        repository.findAll()
            .map { it.toDto() }
    }

    override fun doesExist(key: K): Mono<Boolean> = blockingToMono { key.getId() }
        .zipWhen { id ->
            blockingToMono {
                repository.findById(id).isPresent
            }
        }
        .filter { (_, isPresentedInDb) ->
            isPresentedInDb
        }
        .flatMap { (id, _) ->
            storage.doesExist(id)
                .filter { !it }
                .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
                    "The key $key is presented in database, but missed in storage"
                }
        }
        .defaultIfEmpty(false)

    override fun contentSize(key: K): Mono<Long> = storage.contentSize(key.getId())

    override fun lastModified(key: K): Mono<Instant> = storage.lastModified(key.getId())

    override fun delete(key: K): Mono<Boolean> = blockingToMono { getEntity(key) }
        .flatMap { entity ->
            storage.delete(entity.requiredId())
                .asyncEffectIf({ this }) {
                    blockingToMono {
                        beforeDelete(entity)
                        repository.deleteById(entity.requiredId())
                    }
                }
        }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = blockingToMono {
        repository.save(key.toEntity())
    }
        .flatMap { entity ->
            storage.upload(entity.requiredId(), content)
                .flatMap { contentSize ->
                    blockingToMono {
                        if (contentSize > 0) {
                            repository.save(entity.updateByContentSize(contentSize))
                        } else {
                            repository.delete(entity)
                        }
                    }
                        .thenReturn(contentSize)
                }
        }

    override fun download(key: K): Flux<ByteBuffer> =  blockingToMono { key.getId() }
        .flatMapMany { storage.download(it) }

    private fun getEntity(dto: K): E {
        val result = dto.id?.let { id ->
            repository.findByIdOrNull(id)
                .orNotFound { "Failed to find entity for $this by id = $id" }
        } ?: findByDto(dto)
        return result ?: throw IllegalArgumentException("DTO $this is not saved: ID is not set and failed to find by default example")
    }

    private fun K.getId(): Long = getEntity(this).requiredId()

    /**
     * A default implementation uses Spring's [Example]
     *
     * @param dto
     * @return [E] entity found by [K] dto or null
     */
    protected open fun findByDto(dto: K): E? = repository.findOne(Example.of(dto.toEntity()))
        .orElseGet(null)

    abstract fun K.toEntity(): E

    /**
     * @receiver [E] entity which needs to be processed before deletion
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