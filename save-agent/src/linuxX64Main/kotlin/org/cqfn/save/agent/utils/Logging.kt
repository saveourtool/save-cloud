package org.cqfn.save.agent.utils

import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logInfo
import platform.linux.__NR_gettid
import platform.posix.syscall

fun logInfoCustom(msg: String) = logInfo(
    "${syscall(__NR_gettid.toLong())} $msg"
)

fun logDebugCustom(msg: String) = logDebug(
    "${syscall(__NR_gettid.toLong())} $msg"
)
