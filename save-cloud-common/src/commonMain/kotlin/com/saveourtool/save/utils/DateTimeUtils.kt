/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.save.utils

import kotlinx.datetime.*

/**
 * @return [Instant] from epoch time
 */
fun Long.secondsToInstant(): Instant = Instant.fromEpochMilliseconds(this)

/**
 * Function to convert long number of seconds to LocalDateTime
 *
 * @return an instance of [LocalDateTime]
 */
fun Long.secondsToLocalDateTime(): LocalDateTime = secondsToInstant().toLocalDateTimeAtUtc()

/**
 * @return pretty string representation of [Instant]
 */
fun Instant.prettyPrint() = this.toString()
    .replace("T", " ")
    .replace("Z", "")
    .replace("-", ".")

/**
 * @return [LocalDateTime] in UTC timezone
 */
fun Instant.toLocalDateTimeAtUtc(): LocalDateTime = toLocalDateTime(TimeZone.UTC)

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
