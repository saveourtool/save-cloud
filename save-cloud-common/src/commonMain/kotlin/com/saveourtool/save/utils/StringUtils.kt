@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.utils

/**
 * @return short [String] for list values
 */
fun List<String>?.listToShortString(): String {
    this?.run {
        return if (size <= 2) {
            this.joinToString(" ,")
        } else {
            "${first()} ... ${last()}"
        }
    } ?: return ""
}
