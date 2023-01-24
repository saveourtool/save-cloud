/**
 * Utility methods related to a Date and Time
 */

package com.saveourtool.save.utils

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
 * @return pretty string representation of [LocalDateTime]
 */
fun LocalDateTime.prettyPrint() = this.toString()
    .replace("T", " ")
    .replace("Z", "")
    .replace("-", ".")

/**
 * @return current local date-time in UTC timezone
 */
fun getCurrentLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
