/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.save.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

actual typealias LocalDateTime = kotlinx.datetime.LocalDateTime

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
