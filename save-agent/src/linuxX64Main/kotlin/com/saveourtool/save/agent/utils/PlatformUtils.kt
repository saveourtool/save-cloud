/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.agent.utils

import platform.posix.SIGTERM
import platform.posix.exit
import platform.posix.signal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction

@OptIn(ExperimentalForeignApi::class)
internal actual fun handleSigterm() {
    signal(SIGTERM, staticCFunction<Int, Unit> {
        logInfoCustom("Agent is shutting down because SIGTERM has been received")
        exit(1)
    })
}
