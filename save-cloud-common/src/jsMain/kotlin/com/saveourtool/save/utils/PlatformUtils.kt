/**
 * Platform dependent utility methods
 */

package com.saveourtool.save.utils

actual fun getenv(envName: String): String? = throw NotImplementedError(NOT_IMPLEMENTED_ON_JS)
