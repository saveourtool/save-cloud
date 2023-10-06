@file:Suppress("FILE_NAME_MATCH_CLASS")
/**
 * Platform dependent utility methods
 */

@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.save.utils

actual fun getenv(envName: String): String? = System.getProperty(envName) ?: System.getenv(envName)
