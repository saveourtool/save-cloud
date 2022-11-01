/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.save.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

actual typealias LocalDateTime = kotlinx.datetime.LocalDateTime

/**
 * @return [LocalDateTime] from epoch time
 */
fun Long.secondsToInstant(): Instant = Instant.fromEpochMilliseconds(this)

/**
 * @return pretty string representation of [Instant]
 */
fun Instant.prettyPrint() = this.toString()
    .replace("T", " ")
    .replace("Z", "")
    .replace("-", ".")

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
