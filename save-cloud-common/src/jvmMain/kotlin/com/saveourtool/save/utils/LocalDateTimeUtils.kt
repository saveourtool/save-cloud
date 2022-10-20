@file:Suppress("MagicNumber")

/**
 * Utilities for LocalDateTime
 */

package com.saveourtool.save.utils

/**
 * @param data: year, month, day
 * @return [data] in LocalDateTime format
 */
fun createLocalDateTime(data: Triple<Int, Int, Int>): LocalDateTime {
    val year = data.first
    val month = data.second + 1
    val day = data.third
    return LocalDateTime.of(year, month, day, 0, 0, 0)
}

/**
 * @param data: year, month, day
 * @return [data] in LocalDateTime format plus 1 day
 */
fun getEndLocalDateTime(data: Triple<Int, Int, Int>): LocalDateTime {
    val year = data.first
    val month = data.second + 1
    val day = data.third
    return LocalDateTime.of(year, month, day, 23, 59, 59)
}
