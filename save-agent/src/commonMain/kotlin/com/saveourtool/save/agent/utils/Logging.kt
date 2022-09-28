/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package com.saveourtool.save.agent.utils

import io.ktor.client.plugins.logging.*

internal val ktorLogger = object : Logger {
    override fun log(message: String) {
        logInfoCustom("[HTTP Client] $message")
    }
}

expect fun logErrorCustom(msg: String)

expect fun logInfoCustom(msg: String)

expect fun logDebugCustom(msg: String)
