package com.saveourtool.save.agent.utils

/**
 * It uses [System.setProperty]
 *
 * @param envName
 * @param value
 */
internal actual fun setenv(envName: String, value: String) {
    System.setProperty(envName, value)
}
