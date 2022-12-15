/**
 * Utility methods related to a Date and Time in JVM
 */

package com.saveourtool.save.utils

import kotlinx.datetime.toJavaInstant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Function to convert long number of seconds to LocalDateTime
 *
 * @return an instance of [LocalDateTime]
 */
fun Long.secondsToJLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(secondsToInstant().toJavaInstant(), ZoneOffset.UTC)