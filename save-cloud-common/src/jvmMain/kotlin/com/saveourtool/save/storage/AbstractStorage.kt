package com.saveourtool.save.storage

import com.saveourtool.save.utils.*

import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

import java.util.concurrent.atomic.AtomicBoolean
import javax.annotation.PostConstruct

/**
 * Base implementation of storage with init method
 */
abstract class AbstractStorage<K : Any, R : StorageProjectReactor<K>, U : StoragePreSignedUrl<K>> : Storage<K> {
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
     * [StorageProjectReactor] for this storage
     */
    protected abstract val storageProjectReactor: R

    /**
     * [StoragePreSignedUrl] for this storage
     */
    protected abstract val storagePreSignedUrl: U

    /**
     * Init method
     */
    @PostConstruct
    fun init() {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doInitAsync(storageProjectReactor)
            .thenReturn(true)  // doInit worked
            .defaultIfEmpty(false)  // doInit is emtpy
            .doOnNext { wasDoInitCalled ->
                require(!isInitFinished.compareAndExchange(false, true)) {
                    "Init method cannot be called more than 1 time. Initialization already finished by another project"
                }
                if (wasDoInitCalled) {
                    log.info {
                        "Initialization $storageName is done"
                    }
                }
            }
            .subscribeOn(initScheduler)
            .subscribe()
    }

    /**
     * @param storageProjectReactor
     * @return [Mono] without value, it's [Mono.empty] by default
     */
    protected open fun doInitAsync(storageProjectReactor: R): Mono<Unit> = Mono.empty()

    private fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinished.get()) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    override fun usingProjectReactor(): R = validateAndRun { storageProjectReactor }

    override fun <T : Any> usingProjectReactor(function: StorageProjectReactor<K>.() -> T): T  = validateAndRun {
        function(storageProjectReactor)
    }

    override fun usingPreSignedUrl(): U = validateAndRun { storagePreSignedUrl }

    override fun <T : Any> usingPreSignedUrl(function: StoragePreSignedUrl<K>.() -> T): T  = validateAndRun {
        function(storagePreSignedUrl)
    }
}
