package com.saveourtool.save.agent

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.FileSystem
import okio.Path

internal suspend fun HttpClient.downloadTestResources(target: Path, executionId: String) {
    val bytes = post {
        url("/test-suites-sources/download-snapshot-by-execution-id?executionId=$executionId")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
    }
//        .bodyAsChannel()
        .body<ByteArray>()
    FileSystem.SYSTEM.write(
        target.resolve("archive.zip"),
        mustCreate = true,
    ) {
        write(bytes)
    }
    // todo: unzip
}

internal suspend fun HttpClient.downloadAdditionalResources(targetDirectory: Path) {
    val fileContentBytes = post {
        url("/files/{organizationName}/{projectName}/download")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
    }
        .body<ByteArray>()
    FileSystem.SYSTEM.write(
        targetDirectory.resolve()
    ) {
        write(fileContentBytes)
    }
}
