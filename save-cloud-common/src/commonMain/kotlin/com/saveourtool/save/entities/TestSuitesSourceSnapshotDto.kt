package com.saveourtool.save.entities

import kotlinx.datetime.LocalDateTime

data class TestSuitesSourceSnapshotDto(
    val organizationName: String,
    val testSuitesSourceName: String,
    val version: String,
    val creationTime: LocalDateTime,

    val location: String,
)
