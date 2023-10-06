/**
 * Module with defined constants
 */

package com.saveourtool.save.utils

/**
 * Link to save-cloud on GitHub
 */
const val SAVE_CLOUD_GITHUB_URL = "https://github.com/saveourtool/save-cloud"

/**
 * Delimiter used in database
 */
const val DATABASE_DELIMITER = ","

/**
 * Delimiter used to combine a list to show to user
 */
const val PRETTY_DELIMITER = ", "

/**
 * Delimiter used in URL path
 */
const val URL_PATH_DELIMITER = "/"

/**
 * Period in ms for debounce on frontend
 */
const val DEFAULT_DEBOUNCE_PERIOD = 250

/**
 * Period in ms for ace editor debouncing
 */
const val DEBOUNCE_PERIOD_FOR_EDITORS = 250

/**
 * Number of characters of git commit hash that should be displayed
 */
const val GIT_HASH_PREFIX_LENGTH = 6

/**
 * Part name for multipart-data uploading a file
 */
const val FILE_PART_NAME = "file"

/**
 * A custom header for `Content-Length`
 */
const val CONTENT_LENGTH_CUSTOM = "Content-Length-Custom"

/**
 * Default time to execute setup.sh
 */
const val DEFAULT_SETUP_SH_TIMEOUT_MILLIS: Long = 60_000L
