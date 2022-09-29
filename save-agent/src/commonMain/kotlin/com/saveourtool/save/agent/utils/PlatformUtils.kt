/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.AgentEnvName
import com.saveourtool.save.core.logging.logDebug

/**
 * Atomic values
 */
expect class AtomicLong(value: Long) {
    /**
     * @return value
     */
    fun get(): Long

    /**
     * @param newValue
     */
    fun set(newValue: Long)

    /**
     * @param delta increments the value_ by delta
     * @return the new value
     */
    fun addAndGet(delta: Long): Long
}

/**
 *  Class that holds value and shares atomic reference to the value
 *
 *  @param valueToStore value to store
 */
@Suppress("USE_DATA_CLASS")
expect class GenericAtomicReference<T>(valueToStore: T) {
    /**
     * @return stored value
     */
    fun get(): T

    /**
     * @param newValue new value to store
     */
    fun set(newValue: T)
}

/**
 * @param envName
 * @return env variable name
 */
internal expect fun getenv(envName: String): String?

/**
 * Get value of environment variable [envName] or throw if it is not set.
 *
 * @param envName name of the environment variable
 * @return value of the environment variable
 */
internal fun requiredEnv(envName: AgentEnvName): String = requireNotNull(getenv(envName.name)) {
    "Environment variable $envName is not set but is required"
}.toString()

/**
 * Get value of environment variable [envName] or null.
 *
 * @param envName name of the optional environment variable
 * @return value of the optional environment variable or null
 */
internal fun optionalEnv(envName: AgentEnvName): String? = getenv(envName.name)
    .also {
        it ?: logDebug("Optional environment variable $envName is not provided")
    }
    ?.toString()

/**
 * Process sigterm signal
 */
internal expect fun handleSigterm()
