/**
 * Utilities to work with Linux-specific calls
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.core.logging.logDebug
import platform.posix.getenv

import kotlinx.cinterop.toKString

/**
 * Get value of environment variable [envName] or throw if it is not set.
 *
 * @param envName name of the environment variable
 * @return value of the environment variable
 */
internal fun requiredEnv(envName: AgentEnvName): String = requireNotNull(getenv(envName.name)) {
    "Environment variable $envName is not set but is required"
}.toKString()

/**
 * Get value of environment variable [envName] or null.
 *
 * @param envName name of the optional environment variable
 * @return value of the optional environment variable or null
 */
internal fun optionalEnv(envName: AgentEnvName): String? = getenv(envName.name)
    .also {
        if (it == null) {
            logDebug("Optional environment variable $envName is not provided")
        }
    }
    ?.toKString()
