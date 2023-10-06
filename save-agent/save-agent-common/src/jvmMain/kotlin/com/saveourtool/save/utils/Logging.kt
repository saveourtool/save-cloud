/**
 * Logging utilities for save-agent
 */

@file:JvmName("LoggingJVM")

package com.saveourtool.save.utils

internal actual fun getThreadId() = Thread.currentThread().id
