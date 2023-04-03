/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.save.utils

import kotlin.time.Duration
import kotlinx.datetime.*

/**
 * @return [Instant] from epoch time in mills
 */
fun Long.millisToInstant(): Instant = Instant.fromEpochMilliseconds(this)

/**
 * @return [Instant] from epoch time in seconds
 */
fun Long.secondsToInstant(): Instant = Instant.fromEpochSeconds(this)

/**
 * @param timeZone timezone to print the date time in
 * @return pretty string representation of [LocalDateTime]
 */
fun LocalDateTime.prettyPrint(timeZone: TimeZone = TimeZone.UTC) = toInstant(TimeZone.UTC).toLocalDateTime(timeZone)
    .toString()
    .replace("T", " ")
    .replace("Z", "")
    .replace("-", ".")

/**
 * @param duration
 */
operator fun LocalDateTime.plus(duration: Duration) = toInstant(TimeZone.UTC).plus(duration).toLocalDateTime(TimeZone.UTC)

/**
 * @param duration
 */
operator fun LocalDateTime.minus(duration: Duration) = toInstant(TimeZone.UTC).minus(duration).toLocalDateTime(TimeZone.UTC)

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
