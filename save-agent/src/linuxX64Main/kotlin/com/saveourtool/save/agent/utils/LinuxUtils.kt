/**
 * Utilities to work with Linux-specific calls
 */

package com.saveourtool.save.agent.utils

import platform.posix.getenv

import kotlinx.cinterop.toKString

/**
 * Get value of environment variable [name] or throw if it is not set.
 *
 * @param name name of the environment variable
 * @return value of the environment variable
 */
internal fun requiredEnv(name: String): String = requireNotNull(getenv(name)) {
    "Environment variable $name is not set but is required"
}.toKString()
