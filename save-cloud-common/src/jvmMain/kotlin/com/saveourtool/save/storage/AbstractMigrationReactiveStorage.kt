package com.saveourtool.save.storage

import com.saveourtool.save.utils.*
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import javax.annotation.PostConstruct

/**
 * Abstract storage which has an init method to migrate keys from old storage to new one
 *
 * @property newStorageProjectReactor [StorageProjectReactor] for new storage
 * @property oldStorageProjectReactor [StorageProjectReactor] for old storage
 * @property newStoragePreSignedUrl [StoragePreSignedUrl] for new storage
 */
abstract class AbstractMigrationReactiveStorage<O : Any, N : Any>(
    private val newStorageProjectReactor: StorageProjectReactor<N>,
    private val oldStorageProjectReactor: StorageProjectReactor<O>,
    private val newStoragePreSignedUrl: StoragePreSignedUrl<N>,
) : ReactiveStorage<O> {
    private val log: Logger = getLogger(this::class)
    private val initializer: StorageInitializer = StorageInitializer(this::class)

    /**
     * Init method which copies file from one storage to another
     */
    @PostConstruct
    fun init() {
        initializer.initReactively {
            oldStorageProjectReactor.list()
                .flatMap { migrateKey(it) }
                .thenJust(Unit)
                .defaultIfEmpty(Unit)
        }
    }

    private fun migrateKey(oldKey: O): Mono<Boolean> = blockingToMono { oldKey.toNewKey() }
        .filterWhen { newKey ->
            newStorageProjectReactor.doesExist(newKey)
                .map { existedInNewStorage ->
                    if (existedInNewStorage) {
                        log.debug {
                            "$oldKey from old storage already existed in new storage as $newKey"
                        }
                    }
                    !existedInNewStorage
                }
        }
        .zipWith(oldStorageProjectReactor.contentLength(oldKey))
        .flatMap { (newKey, contentSize) ->
            newStorageProjectReactor.upload(newKey, contentSize, oldStorageProjectReactor.download(oldKey))
                .map {
                    log.info {
                        "Copied $oldKey to new storage with key $newKey"
                    }
                }
                .flatMap {
                    oldStorageProjectReactor.delete(oldKey)
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

    override fun list(): Flux<O> = initializer.validateAndRun { newStorageProjectReactor.list().map { it.toOldKey() } }

    override fun download(key: O): Flux<ByteBuffer> = initializer.validateAndRun { newStorageProjectReactor.download(key.toNewKey()) }

    override fun upload(key: O, content: Flux<ByteBuffer>): Mono<O> = initializer.validateAndRun { newStorageProjectReactor.upload(key.toNewKey(), content).map { it.toOldKey() } }

    override fun upload(key: O, contentLength: Long, content: Flux<ByteBuffer>): Mono<O> =
            initializer.validateAndRun { newStorageProjectReactor.upload(key.toNewKey(), contentLength, content).map { it.toOldKey() } }

    override fun delete(key: O): Mono<Boolean> = initializer.validateAndRun { newStorageProjectReactor.delete(key.toNewKey()) }

    override fun lastModified(key: O): Mono<Instant> = initializer.validateAndRun { newStorageProjectReactor.lastModified(key.toNewKey()) }

    override fun contentLength(key: O): Mono<Long> = initializer.validateAndRun { newStorageProjectReactor.contentLength(key.toNewKey()) }

    override fun doesExist(key: O): Mono<Boolean> = initializer.validateAndRun { newStorageProjectReactor.doesExist(key.toNewKey()) }

    override fun move(source: O, target: O): Mono<Boolean> = initializer.validateAndRun { newStorageProjectReactor.move(source.toNewKey(), target.toNewKey()) }

    override fun generateUrlToDownload(key: O): URL? = initializer.validateAndRun { newStoragePreSignedUrl.generateUrlToDownload(key.toNewKey()) }
}
