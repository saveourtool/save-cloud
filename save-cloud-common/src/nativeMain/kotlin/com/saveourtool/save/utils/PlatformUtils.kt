/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

import kotlinx.cinterop.toKString

actual fun getenv(envName: String): String? = platform.posix.getenv(envName)?.toKString()
