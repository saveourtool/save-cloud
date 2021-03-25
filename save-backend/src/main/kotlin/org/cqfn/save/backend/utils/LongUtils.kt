/**
 * Util class for Long type
 */
package org.cqfn.save.backend.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Function to convert long to LocalDateTime
 */
fun Long.toLocalDateTime() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
