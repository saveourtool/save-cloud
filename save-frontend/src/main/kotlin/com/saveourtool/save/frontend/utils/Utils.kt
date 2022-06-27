/**
 * Various utils for frontend
 */

package com.saveourtool.save.frontend.utils

import com.saveourtool.save.domain.FileInfo
import com.saveourtool.save.domain.Role

import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData
import react.RBuilder
import react.dom.br
import react.dom.samp
import react.dom.small
import react.dom.table
import react.dom.tbody
import react.dom.td
import react.dom.tr

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * @return a nicely formatted string representation of [FileInfo]
 */
@Suppress("MAGIC_NUMBER", "MagicNumber")
fun FileInfo.toPrettyString() = "$name (uploaded at ${
    Instant.fromEpochMilliseconds(uploadedMillis).toLocalDateTime(
        TimeZone.UTC
    )
}, size ${sizeBytes / 1024} KiB)"

/**
 * Append an object [obj] to `this` [FormData] as a JSON, using kx.serialization for serialization
 *
 * @param name key to be appended to the form data
 * @param obj an object to be appended
 * @return Unit
 */
inline fun <reified T> FormData.appendJson(name: String, obj: T) =
        append(
            name,
            Blob(
                arrayOf(Json.encodeToString(obj)),
                BlobPropertyBag("application/json")
            )
        )

/**
 * Adds this text to RBuilder line by line, separating with `<br>`
 *
 * @param text text to display
 */
@Suppress("EMPTY_BLOCK_STRUCTURE_ERROR")
internal fun RBuilder.multilineText(text: String) {
    text.lines().forEach {
        small {
            samp {
                +it
            }
        }
        br { }
    }
}

/**
 * @param text
 */
internal fun RBuilder.multilineTextWithIndices(text: String) {
    table("table table-borderless table-hover table-sm") {
        tbody {
            text.lines().filterNot { it.isEmpty() }.forEachIndexed { i, line ->
                tr {
                    td {
                        +"${i + 1}"
                    }
                    td {
                        +line
                    }
                }
            }
        }
    }
}

/**
 * @param maxLength
 * @return true if string is invalid
 */
internal fun String?.isInvalid(maxLength: Int) = this.isNullOrBlank() || this.contains(" ") || this.length > maxLength

/**
 * @return [Role] if string matches any role, else throws [IllegalStateException]
 * @throws IllegalStateException if string is not matched with any role
 */
fun String.toRole() = Role.values().find {
    this == it.formattedName || this == it.toString()
} ?: throw IllegalStateException("Unknown role is passed: $this")
