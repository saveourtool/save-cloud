package com.saveourtool.save.storage

import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.info

import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.reflect.KClass

/**
 * Initializer for [StorageProjectReactor]
 *
 * @property storageName
 */
class StorageInitializer(
    private val storageName: String,
) {
    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitStarted = AtomicBoolean(false)

    @SuppressWarnings("NonBooleanPropertyPrefixedWithIs")
    private val isInitFinished = AtomicBoolean(false)

    /**
     * @param clazz [storageName] will be calculated from class name
     */
    constructor(clazz: KClass<*>) : this(clazz.simpleName ?: clazz.java.simpleName)

    /**
     * Init method using method that returns [Mono]
     * It can be empty
     *
     * @param doInit
     */
    fun init(doInit: () -> Mono<Unit>) {
        require(!isInitStarted.compareAndExchange(false, true)) {
            "Init method cannot be called more than 1 time, initialization is in progress"
        }
        doInit()
            .map { true }  // doInit worked
            .defaultIfEmpty(false)  // doInit is emtpy
            .doOnNext { wasDoInitCalled ->
                require(!isInitFinished.compareAndExchange(false, true)) {
                    "Init method cannot be called more than 1 time. Initialization $storageName already finished by another run"
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
     * @param action
     * @return result of [action] if initialization is finished, otherwise -- exception
     */
    fun <R> validateAndRun(action: () -> R): R {
        require(isInitFinished.get()) {
            "Any method of $storageName should be called after init method is finished"
        }
        return action()
    }

    companion object {
        private val log: Logger = getLogger<StorageInitializer>()
        private val initScheduler: Scheduler = Schedulers.boundedElastic()
    }
}
