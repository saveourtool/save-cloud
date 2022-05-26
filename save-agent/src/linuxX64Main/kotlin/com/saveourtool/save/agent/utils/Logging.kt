/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package com.saveourtool.save.agent.utils

import com.saveourtool.save.core.logging.logDebug
import com.saveourtool.save.core.logging.logError
import com.saveourtool.save.core.logging.logInfo

import platform.linux.__NR_gettid
import platform.posix.syscall

fun logErrorCustom(msg: String) = logError(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logInfoCustom(msg: String) = logInfo(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logDebugCustom(msg: String) = logDebug(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)
