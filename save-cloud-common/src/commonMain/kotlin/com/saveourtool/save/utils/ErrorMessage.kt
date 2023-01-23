package com.saveourtool.save.utils

/**
 * An error message, intended to be assignment-incompatible with the regular
 * string.
 *
 * @property message the error message.
 */
class ErrorMessage(val message: String) {
    override fun toString(): String =
            message
}
