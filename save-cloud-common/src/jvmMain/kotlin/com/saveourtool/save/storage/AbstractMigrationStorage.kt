package com.saveourtool.save.storage

import com.saveourtool.save.utils.*
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

/**
 * Abstract storage which has an init method to migrate keys from old storage to new one
 */
abstract class AbstractMigrationStorage<O : Any, N : Any>(
    private val oldStorage: Storage<O>,
    private val newStorage: Storage<N>,
) : Storage<O> {
    private val log: Logger = getLogger(this.javaClass)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isMigrationStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isMigrationFinished = AtomicBoolean(false)

    /**
     * Init method which copies file from one storage to another
     */
    @PostConstruct
    fun migrate() {
        migrateAsync().subscribe()
    }

    /**
     * Async method which copies file from one storage to another
     *
     * @return [Mono] without value
     */
    fun migrateAsync(): Mono<Unit> {
        require(!isMigrationStarted.compareAndExchange(false, true)) {
            "Migration cannot be called more than 1 time, migration is in progress"
        }
        return oldStorage.list()
            .flatMap { migrateKey(it) }
            .switchIfEmpty(true.toMono())
            .then(
                Mono.fromCallable {
                    require(!isMigrationFinished.compareAndExchange(false, true)) {
                        "Migration cannot be called more than 1 time. Migration already finished by another project"
                    }
                    log.info {
                        "Migration of ${javaClass.simpleName} is done"
                    }
                }
            )
    }

    private fun migrateKey(oldKey: O): Mono<Boolean> = blockingToMono { oldKey.toNewKey() }
        .filterWhen { newKey ->
            newStorage.doesExist(newKey)
                .map { existedInNewStorage ->
                    if (existedInNewStorage) {
                        log.debug {
                            "$oldKey from old storage already existed in new storage as $newKey"
                        }
                    }
                    !existedInNewStorage
                }
        }
        .zipWith(oldStorage.contentLength(oldKey))
        .flatMap { (newKey, contentSize) ->
            newStorage.upload(newKey, contentSize, oldStorage.download(oldKey))
                .map {
                    log.info {
                        "Copied $oldKey to new storage with key $newKey"
                    }
                }
                .flatMap {
                    oldStorage.delete(oldKey)
                }
        }
        .onErrorResume { ex ->
            Mono.fromCallable {
                log.warn(ex) {
                    "Failed to copy $oldKey from old storage"
                }
                false
            }
        }

    /**
     * @receiver [O] old key
     * @return [N] new key created from receiver
     */
    protected abstract fun O.toNewKey(): N

    /**
     * @receiver [N] new key
     * @return [O] old key created from receiver
     */
    protected abstract fun N.toOldKey(): O

    private fun <R> validateAndRun(action: () -> R): R {
        require(isMigrationFinished.get()) {
            "Any method of ${javaClass.simpleName} should be called after migration is finished"
        }
        return action()
    }

    override fun list(): Flux<O> = validateAndRun { newStorage.list().map { key -> key.toOldKey() } }

    override fun download(key: O): Flux<ByteBuffer> = validateAndRun { newStorage.download(key.toNewKey()) }

    override fun upload(key: O, content: Flux<ByteBuffer>): Mono<Long> = validateAndRun { newStorage.upload(key.toNewKey(), content) }

    override fun upload(key: O, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = validateAndRun { newStorage.upload(key.toNewKey(), contentLength, content) }

    override fun delete(key: O): Mono<Boolean> = validateAndRun { newStorage.delete(key.toNewKey()) }

    override fun lastModified(key: O): Mono<Instant> = validateAndRun { newStorage.lastModified(key.toNewKey()) }

    override fun contentLength(key: O): Mono<Long> = validateAndRun { newStorage.contentLength(key.toNewKey()) }

    override fun doesExist(key: O): Mono<Boolean> = validateAndRun { newStorage.doesExist(key.toNewKey()) }

    override fun move(source: O, target: O): Mono<Boolean> = validateAndRun { newStorage.move(source.toNewKey(), target.toNewKey()) }
}
