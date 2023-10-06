/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

import com.saveourtool.save.core.utils.GenericAtomicReference

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlinx.datetime.Clock

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
