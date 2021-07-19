/**
 * Util class for Long type
 */

package org.cqfn.save.backend.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Function to convert long number of seconds to LocalDateTime
 *
 * @return an instance of LocalDateTime
 */
fun Long.secondsToLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(this), ZoneOffset.UTC)
