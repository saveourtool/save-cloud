/**
 * Logging utilities for save-agent
 */

@file:Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_ON_FUNCTION")

package com.saveourtool.save.agent.utils

import platform.linux.__NR_gettid
import platform.posix.syscall

internal actual fun getThreadId() = syscall(__NR_gettid.toLong())
