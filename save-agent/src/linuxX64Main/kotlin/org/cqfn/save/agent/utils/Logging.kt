/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package org.cqfn.save.agent.utils

import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo

import platform.linux.__NR_gettid
import platform.posix.syscall

fun CoroutineScope.logErrorCustom(msg: String) = logError(
    "[tid ${syscall(__NR_gettid.toLong())}] [ctx ${worker()}] $msg"
)

fun CoroutineScope.logInfoCustom(msg: String) = logInfo(
    "[tid ${syscall(__NR_gettid.toLong())}] [ctx ${worker()}] $msg"
)

fun CoroutineScope.logDebugCustom(msg: String) = logDebug(
    "[tid ${syscall(__NR_gettid.toLong())}] [ctx ${worker()}] $msg"
)

fun logErrorCustom(msg: String) = logError(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logInfoCustom(msg: String) = logInfo(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

fun logDebugCustom(msg: String) = logDebug(
    "[tid ${syscall(__NR_gettid.toLong())}] $msg"
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun CoroutineScope.worker() = (coroutineContext as? CloseableCoroutineDispatcher)?.worker
