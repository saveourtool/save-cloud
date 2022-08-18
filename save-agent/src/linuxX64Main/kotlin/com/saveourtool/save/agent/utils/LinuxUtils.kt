package com.saveourtool.save.agent.utils

import kotlinx.cinterop.toKString
import platform.posix.getenv

internal fun requiredEnv(name: String): String = requireNotNull(getenv(name)) {
    "Environment variable $name is not set but is required"
}.toKString()
