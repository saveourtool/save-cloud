/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.agent.utils

import platform.posix.SIGTERM
import platform.posix.exit
import platform.posix.signal

import kotlinx.cinterop.staticCFunction

@Suppress("MemberNameEqualsClassName")
actual class AtomicLong actual constructor(value: Long) {
    private val atomicLong = kotlin.native.concurrent.AtomicLong(value)

    actual fun get(): Long = atomicLong.value

    actual fun set(newValue: Long) {
        atomicLong.value = newValue
    }

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

internal actual fun handleSigterm() {
    signal(SIGTERM, staticCFunction<Int, Unit> {
        logInfoCustom("Agent is shutting down because SIGTERM has been received")
        exit(1)
    })
}
