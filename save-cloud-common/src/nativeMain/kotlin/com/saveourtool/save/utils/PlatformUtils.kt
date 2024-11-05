/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

actual class AtomicLong actual constructor(value: Long) {
    private val kotlinAtomicLong = kotlin.concurrent.AtomicLong(value)

    actual fun get(): Long = kotlinAtomicLong.value

    actual fun set(newValue: Long) {
        kotlinAtomicLong.value = newValue
    }

    actual fun addAndGet(delta: Long): Long = kotlinAtomicLong.addAndGet(delta)
}

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: kotlin.concurrent.AtomicReference<T> = kotlin.concurrent.AtomicReference(valueToStore)
    actual fun get(): T = holder.value
    actual fun set(newValue: T) {
        holder.value = newValue
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getenv(envName: String): String? = platform.posix.getenv(envName)?.toKString()
