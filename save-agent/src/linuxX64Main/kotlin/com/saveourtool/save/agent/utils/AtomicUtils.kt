package com.saveourtool.save.agent.utils

/**
 * Atomic values
 */
actual class AtomicLong actual constructor(value: Long) {
    private val atomicLong = kotlin.native.concurrent.AtomicLong(value)

    /**
     * @return value
     */
    actual fun get(): Long = atomicLong.value

    /**
     *
     */
    actual fun set(newValue: Long) {
        atomicLong.value = newValue
    }

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    actual fun addAndGet(delta: Long): Long = atomicLong.addAndGet(delta)
}

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: kotlin.native.concurrent.AtomicReference<T> = kotlin.native.concurrent.AtomicReference(valueToStore)
    actual fun get(): T = holder.value
    actual fun set(newValue: T) {
        holder.value = newValue
    }
}