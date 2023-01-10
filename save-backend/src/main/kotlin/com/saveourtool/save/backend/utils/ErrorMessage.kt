package com.saveourtool.save.backend.utils

/**
 * An error message, intended to be assignment-incompatible with the regular
 * string.
 *
 * @property message the error message.
 */
@JvmInline
value class ErrorMessage(val message: String) {
    override fun toString(): String =
            message
}
