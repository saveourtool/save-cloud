/**
 * Platform dependent utility methods
 */

@file:Suppress("FILE_NAME_MATCH_CLASS")
@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.save.agent.utils

import sun.misc.Signal
import kotlin.system.exitProcess

internal actual fun handleSigterm() {
    Signal.handle(Signal("TERM")) {
        logInfoCustom("Agent is shutting down because SIGTERM has been received")
        exitProcess(1)
    }
}
