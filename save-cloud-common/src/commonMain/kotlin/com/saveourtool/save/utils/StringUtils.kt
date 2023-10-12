@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.utils

const val LOGIN_MAX_LENGTH = 14
const val REAL_NAME_PART_MAX_LENGTH = 20

/**
 * @return short [String] for list values
 */
fun List<String>?.listToShortString(): String = this?.run {
    if (size <= 2) {
        this.joinToString(" ,")
    } else {
        "${first()} ... ${last()}"
    }
}.orEmpty()

/**
 * @param maxLength
 * @return short [String] for text
 */
fun String.shorten(maxLength: Int): String = substring(0, maxLength - 1) + "..."

/**
 * @return short [String] for login
 */
fun String.shortenLogin(): String = shorten(LOGIN_MAX_LENGTH)

/**
 * @return short [String] for real name
 */
fun String.shortenRealName(): String = shorten(REAL_NAME_PART_MAX_LENGTH)
