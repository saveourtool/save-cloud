/**
 * Utilities for Kotlin
 */

package com.saveourtool.save.utils

import kotlinx.coroutines.delay

typealias StringList = List<String>

/**
 * Run [action] several [times] with [timeMillis] milliseconds
 *
 * [T] is just a non-nullable type
 *
 * @param times number of times to retry [action]
 * @param delayMillis number of milliseconds to wait until next retry
 * @param action action that should be invoked
 * @return [T] if the result was fetched in [times] attempts, null otherwise
 */
suspend fun <T : Any> retry(
    times: Int,
    delayMillis: Long = 10_000L,
    action: () -> T?,
): T? = action() ?: run {
    if (times > 0) {
        delay(delayMillis)
        retry(times - 1, delayMillis, action)
    } else {
        null
    }
}
