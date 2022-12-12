/**
 * Platform dependent utility methods
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.save.agent.utils

import sun.misc.Signal
import kotlin.system.exitProcess

actual typealias AtomicLong = java.util.concurrent.atomic.AtomicLong

@Suppress("USE_DATA_CLASS")
actual class GenericAtomicReference<T> actual constructor(valueToStore: T) {
    private val holder: java.util.concurrent.atomic.AtomicReference<T> = java.util.concurrent.atomic.AtomicReference(valueToStore)
    actual fun get(): T = holder.get()
    actual fun set(newValue: T) {
        holder.set(newValue)
    }
}

internal actual fun getenv(envName: String): String? = System.getProperty(envName) ?: System.getenv(envName)

internal actual fun handleSigterm() {
    Signal.handle(Signal("TERM")) {
        logInfoCustom("Agent is shutting down because SIGTERM has been received")
        exitProcess(1)
    }
}
