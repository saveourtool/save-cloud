/**
 * File containing some general utility functions
 */

package com.saveourtool.save.demo.agent.utils

import platform.posix.getenv

import kotlinx.cinterop.toKString

/**
 * @param envName name of environment variable
 * @return environment variable value
 * @throws [IllegalArgumentException] if environment variable is not found
 */
fun getEnvOrNotFound(envName: String) = requireNotNull(getenv(envName)?.toKString()) {
    "Could not get $envName environment variable."
}
