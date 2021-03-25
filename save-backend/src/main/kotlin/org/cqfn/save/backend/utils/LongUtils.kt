package org.cqfn.save.backend.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Util class for Long type
 */

/**
 * Function to convert long to LocalDateTime
 */
fun Long.toLocalTimeDate() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)
