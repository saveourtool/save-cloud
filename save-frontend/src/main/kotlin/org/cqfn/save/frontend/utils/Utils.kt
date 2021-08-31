package org.cqfn.save.frontend.utils

import org.cqfn.save.domain.FileInfo
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.xhr.FormData

fun FileInfo.toPrettyString() = "$name (uploaded at ${
    Instant.fromEpochMilliseconds(uploadedMillis).toLocalDateTime(
        TimeZone.UTC
    )
}, size ${sizeBytes / 1024} KiB)"

inline fun <reified T> FormData.appendJson(name: String, obj: T) =
    append(
        name,
        Blob(
            arrayOf(Json.encodeToString(obj)),
            BlobPropertyBag("application/json")
        )
    )
