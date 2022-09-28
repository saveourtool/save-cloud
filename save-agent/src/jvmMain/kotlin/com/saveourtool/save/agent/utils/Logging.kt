@file:JvmName("LoggingJVM")

package com.saveourtool.save.agent.utils

import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo

actual fun logErrorCustom(msg: String) = logError(
    "[tid ${Thread.currentThread().id}] $msg"
)

actual fun logInfoCustom(msg: String) = logInfo(
    "[tid ${Thread.currentThread().id}] $msg"
)

actual fun logDebugCustom(msg: String) = logDebug(
    "[tid ${Thread.currentThread().id}] $msg"
)
