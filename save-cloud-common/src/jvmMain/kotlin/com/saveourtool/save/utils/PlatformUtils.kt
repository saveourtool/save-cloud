/**
 * Platform dependent utility methods
 */

@file:JvmName("PlatformUtilsJVM")

package com.saveourtool.save.utils

actual fun getenv(envName: String): String? = System.getProperty(envName) ?: System.getenv(envName)
