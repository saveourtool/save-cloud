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
 * @return short [String] for login
 */
fun String?.shortenLogin(): String = this?.substring(0, LOGIN_MAX_LENGTH - 1) + "..."

/**
 * @return short [String] for real name
 */
fun String?.shortenRealName(): String = this?.substring(0, REAL_NAME_PART_MAX_LENGTH - 1) + "..."
