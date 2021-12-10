/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package org.cqfn.save.agent.utils

import platform.linux.__NR_gettid
import platform.posix.syscall

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun logErrorCustom(msg: String) = logMessage(
    "ERROR",
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logInfoCustom(msg: String) = logMessage(
    "INFO",
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logDebugCustom(msg: String) = logMessage(
    "DEBUG",
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

private fun logMessage(
    level: String,
    msg: String,
) {
    val currentTime = run {
        val currentTimeInstance = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        " ${currentTimeInstance.date} ${currentTimeInstance.hour}:${currentTimeInstance.minute}:${currentTimeInstance.second}"
    }
    println("[$level]$currentTime: $msg")
}
