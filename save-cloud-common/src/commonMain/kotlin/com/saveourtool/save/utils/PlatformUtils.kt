/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

import com.saveourtool.save.core.logging.logDebug

/**
 * @param envName
 * @return env variable name
 */
expect fun getenv(envName: String): String?

/**
 * Get value of environment variable [envName] or throw if it is not set.
 *
 * @param envName name of the environment variable
 * @return value of the environment variable
 */
fun requiredEnv(envName: String): String = requireNotNull(getenv(envName)) {
    "Environment variable $envName is not set but is required"
}.toString()

/**
 * Get value of environment variable [envName] or null.
 *
 * @param envName name of the optional environment variable
 * @return value of the optional environment variable or null
 */
fun optionalEnv(envName: String): String? = getenv(envName)
    .also {
        it ?: logDebug("Optional environment variable $envName is not provided")
    }
    ?.toString()
