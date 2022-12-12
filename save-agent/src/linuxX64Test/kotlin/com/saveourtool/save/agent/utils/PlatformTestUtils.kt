package com.saveourtool.save.agent.utils

/**
 * @param envName
 * @param value
 */
internal actual fun setenv(envName: String, value: String) {
    platform.posix.setenv(envName, value, 1)
}
