/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

import com.saveourtool.save.core.utils.GenericAtomicReference
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.datetime.Clock

/**
 * Atomic values
 */
expect class AtomicLong(value: Long) {
    /**
     * @return value
     */
    fun get(): Long

    /**
     * @param newValue
     */
    fun set(newValue: Long)

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    fun addAndGet(delta: Long): Long
}

/**
 *  Class that holds value and shares atomic reference to the value
 *
 *  @param valueToStore value to store
 */
@Suppress("USE_DATA_CLASS")
expect class GenericAtomicReference<T>(valueToStore: T) {
    /**
     * @return stored value
     */
    fun get(): T

    /**
     * @param newValue new value to store
     */
    fun set(newValue: T)
}

/**
 * A wrapper around a value of type [T] that caches it for [expirationTimeSeconds] and then recalculates
 * using [valueGetter]
 *
 * @param expirationTime value expiration time
 * @property valueGetter a function to calculate the value of type [T]
 */
class ExpiringValueWrapper<T : Any>(
    expirationTime: Duration,
    private val valueGetter: () -> T,
) {
    private val expirationTimeSeconds = expirationTime.toLong(DurationUnit.SECONDS)
    private val lastUpdateTimeSeconds = AtomicLong(0)
    private val value: GenericAtomicReference<T> = GenericAtomicReference(valueGetter())
    private val mutex = Mutex()

    /**
     * @return cached value or refreshes the value and returns it
     */
    fun getValue(): T {
        val current = Clock.System.now().epochSeconds
        if (current - lastUpdateTimeSeconds.get() > expirationTimeSeconds) {
            value.set(valueGetter())
            lastUpdateTimeSeconds.set(current)
        }
        return value.get()
    }
}
