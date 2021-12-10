package org.cqfn.save.agent.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
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
