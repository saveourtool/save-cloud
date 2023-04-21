/**
 * Module that contains different error messages.
 */

@file:JsExport

package com.saveourtool.save.validation

import kotlin.js.JsExport

/**
 * Error message that is shown when email input is invalid.
 */
const val EMAIL_ERROR_MESSAGE = "Please input a valid email address"

/**
 * Error message that is shown when url input is invalid.
 */
const val URL_ERROR_MESSAGE = "Please input a valid URL that starts either with [http://] or [https://]"

/**
 * Error message that is shown when name input is invalid.
 */
const val NAME_ERROR_MESSAGE = "Please input a name that doesn't contain anything but english letters, numbers, dots and hyphens (but not the first and the last characters)"

/**
 * Error message that is shown when date range input is invalid.
 */
const val DATE_RANGE_ERROR_MESSAGE = "Please input a valid date range"

/**
 * Error message that is shown when CVE identifier is invalid.
 */
const val CVE_NAME_ERROR_MESSAGE = "CVE identifier is invalid"
