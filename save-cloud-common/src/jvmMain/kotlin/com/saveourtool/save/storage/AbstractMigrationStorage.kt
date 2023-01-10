package com.saveourtool.save.storage

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import com.saveourtool.save.utils.warn
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * Abstract storage which has an init method to migrate keys from old storage to new one
 */
abstract class AbstractMigrationStorage<O : Any, N : Any>(
    private val oldStorage: Storage<O>,
    private val newStorage: Storage<N>,
) : Storage<O> {
    private val log: Logger = getLogger(this.javaClass)
    private val migrationLock = ReentrantReadWriteLock()
    private var isMigrated = false

    /**
     * Init method which copies file from one storage to another
     */
    fun migrate() {
        migrationLock.writeLock().withLock {
            require(!isMigrated) {
                "Migration cannot be called more than 1 time"
            }
            doMigrate()
            isMigrated = true
        }
    }

    private fun doMigrate() {
        oldStorage.list()
            .map { oldKey ->
                oldKey to oldKey.toNewKey()
            }
            .filterWhen { (oldKey, newKey) ->
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
            .flatMap { (oldKey, newKey) ->
                newStorage.upload(newKey, oldStorage.download(oldKey))
                    .map {
                        log.info {
                            "Copied $oldKey to new storage with key $newKey"
                        }
                    }
                    .flatMap {
                        oldStorage.delete(oldKey)
                    }
                    .onErrorResume { ex ->
                        Mono.fromCallable {
                            log.warn(ex) {
                                "Failed to copy $oldKey from old storage"
                            }
                            false
                        }
                    }
            }
            .blockLast()
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

    private fun <R> validateAndRun(action: () -> R): R = migrationLock.readLock().withLock {
        require(isMigrated) {
            "Any method of ${javaClass.simpleName} should be called after migration"
        }
        action()
    }

    override fun list(): Flux<O> = validateAndRun { newStorage.list().map { key -> key.toOldKey() } }

    override fun download(key: O): Flux<ByteBuffer> = validateAndRun { newStorage.download(key.toNewKey()) }

    override fun upload(key: O, content: Flux<ByteBuffer>): Mono<Long> = validateAndRun { newStorage.upload(key.toNewKey(), content) }

    override fun delete(key: O): Mono<Boolean> = validateAndRun { newStorage.delete(key.toNewKey()) }

    override fun lastModified(key: O): Mono<Instant> = validateAndRun { newStorage.lastModified(key.toNewKey()) }

    override fun contentSize(key: O): Mono<Long> = validateAndRun { newStorage.contentSize(key.toNewKey()) }

    override fun doesExist(key: O): Mono<Boolean> = validateAndRun { newStorage.doesExist(key.toNewKey()) }
}
