@file:Suppress("FILE_NAME_MATCH_CLASS")
/**
 * Platform dependent utility methods
 */

@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.save.utils

actual typealias AtomicLong = java.util.concurrent.atomic.AtomicLong

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: java.util.concurrent.atomic.AtomicReference<T> = java.util.concurrent.atomic.AtomicReference(valueToStore)
    actual fun get(): T = holder.get()
    actual fun set(newValue: T) {
        holder.set(newValue)
    }
}
