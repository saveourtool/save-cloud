package org.cqfn.save.frontend.utils

import org.cqfn.save.domain.FileInfo
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun FileInfo.toPrettyString() = "$name (uploaded at ${
    Instant.fromEpochMilliseconds(uploadedMillis).toLocalDateTime(
        TimeZone.UTC
    )
}, size ${sizeBytes / 1024} KiB)"
