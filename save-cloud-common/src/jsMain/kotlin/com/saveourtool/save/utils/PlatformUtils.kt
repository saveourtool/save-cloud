/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

actual class AtomicLong actual constructor(value: Long) {
    actual fun get(): Long = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)

    actual fun set(newValue: Long) {
        throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
    }

    actual fun addAndGet(delta: Long): Long = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
}

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    actual fun get(): T = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
    actual fun set(newValue: T) {
        throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
    }
}

actual fun getenv(envName: String): String? = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
