package com.saveourtool.save.api.errors

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * A time-out was reached while waiting for an operation to complete.
 *
 * @property valueMillis the time-out value, in milliseconds.
 * @property message the detail message.
 */
data class TimeoutError internal constructor(
    override val message: String,
    val valueMillis: Long = -1L
) : SaveCloudError() {
    /**
     * @param value the time-out value.
     * @param unit the time-out unit.
     * @param messageSuffix the optional suffix to be appended to the default
     *   detail [message].
     */
    @Suppress("WRONG_INDENTATION")
    internal constructor(
        value: Long,
        unit: TimeUnit = MILLISECONDS,
        messageSuffix: String = ""
    ) : this(
        "A timeout of $value ${unit.name.lowercase()} has been reached".withSuffix(messageSuffix),
        unit.toMillis(value)
    )

    private companion object {
        private fun String.withSuffix(messageSuffix: String): String =
                when {
                    messageSuffix.isEmpty() -> this
                    else -> "$this $messageSuffix"
                }
    }
}
