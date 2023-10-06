/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package com.saveourtool.save.utils

import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo
import io.ktor.client.plugins.logging.*

val ktorLogger = object : Logger {
    override fun log(message: String) {
        logInfoCustom("[HTTP Client] $message")
    }
}

fun logErrorCustom(msg: String) = logError(
    "[tid ${getThreadId()}] $msg"
)

fun logInfoCustom(msg: String) = logInfo(
    "[tid ${getThreadId()}] $msg"
)

fun logDebugCustom(msg: String) = logDebug(
    "[tid ${getThreadId()}] $msg"
)

internal expect fun getThreadId(): Long
