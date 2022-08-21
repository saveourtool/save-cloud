package com.saveourtool.save.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

actual typealias LocalDateTime = kotlinx.datetime.LocalDateTime

fun getCurrentLocalDateTime() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
