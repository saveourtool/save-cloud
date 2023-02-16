package com.saveourtool.save.storage

import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.asCoroutineDispatcher
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

/**
 * Storage implementation which wraps provided storage and adds an init method
 *
 * @param K type of key
 */
abstract class StorageWrapperWithInit<K> : Storage<K> {
    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitFinished = AtomicBoolean(false)

    /**
     * Logger from child
     */
    protected open val log: Logger = getLogger(this::class)

    /**
     * Storage name, it's class name by default
     */
    protected open val storageName: String = this::class.simpleName ?: this::class.java.simpleName

    private val initScheduler: Scheduler = Schedulers.boundedElastic()

    /**
     * A shared [CoroutineDispatcher] for init methods
     */
    protected val initCoroutineDispatcher: CoroutineDispatcher = initScheduler.asCoroutineDispatcher()

    private val underlying: Storage<K> by lazy { createUnderlyingStorage() }

    /**
     * @return underlying [Storage] storage which needs to wrap by init method
     */
    protected abstract fun createUnderlyingStorage(): Storage<K>

    /**
     * Init method
     */
    @PostConstruct
    fun init() {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doInitAsync(underlying)
            .doOnNext {
                require(!isInitFinished.compareAndExchange(false, true)) {
                    "Init method cannot be called more than 1 time. Initialization already finished by another project"
                }
                log.info {
                    "Initialization of $storageName is done"
                }
            }
            .subscribeOn(initScheduler)
            .subscribe()
    }

    /**
     * Async init method
     *
     * @param underlying wrapped storage
     * @return [Mono] without body
     */
    protected open fun doInitAsync(underlying: Storage<K>): Mono<Unit> = Mono.just(Unit)

    private fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinished.get()) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    private suspend fun <R> validateAndRunSuspend(action: suspend () -> R): R {
        require(isInitFinished.get()) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    override fun list(): Flux<K> = validateAndRun {
        underlying.list()
    }

    override fun doesExist(key: K): Mono<Boolean> = validateAndRun {
        underlying.doesExist(key)
    }

    override fun contentLength(key: K): Mono<Long> = validateAndRun {
        underlying.contentLength(key)
    }

    override fun lastModified(key: K): Mono<Instant> = validateAndRun {
        underlying.lastModified(key)
    }

    override fun delete(key: K): Mono<Boolean> = validateAndRun {
        underlying.delete(key)
    }

    override fun upload(key: K, content: Flux<ByteBuffer>): Mono<Long> = validateAndRun {
        underlying.upload(key, content)
    }

    override fun upload(key: K, contentLength: Long, content: Flux<ByteBuffer>): Mono<Unit> = validateAndRun {
        underlying.upload(key, contentLength, content)
    }

    override suspend fun upload(key: K, contentLength: Long, content: Flow<ByteBuffer>) = validateAndRunSuspend {
        underlying.upload(key, contentLength, content)
    }

    override fun download(key: K): Flux<ByteBuffer> = validateAndRun {
        underlying.download(key)
    }

    override fun generateUrlToDownload(key: K): URL = validateAndRun {
        underlying.generateUrlToDownload(key)
    }

    override fun generateUrlToUpload(key: K, contentLength: Long): UrlWithHeaders = validateAndRun {
        underlying.generateUrlToUpload(key, contentLength)
    }

    override fun move(source: K, target: K): Mono<Boolean> = validateAndRun {
        underlying.move(source, target)
    }
}
