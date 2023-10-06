/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

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
