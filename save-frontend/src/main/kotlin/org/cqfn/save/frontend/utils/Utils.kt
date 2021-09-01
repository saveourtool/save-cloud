/**
 * Various utils for frontend
 */

package org.cqfn.save.frontend.utils

import org.cqfn.save.domain.FileInfo

import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData

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
