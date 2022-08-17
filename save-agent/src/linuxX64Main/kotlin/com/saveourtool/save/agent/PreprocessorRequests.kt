package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.logDebugCustom
import com.saveourtool.save.domain.FileKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.FileSystem
import okio.Path

internal suspend fun HttpClient.downloadTestResources(baseUrl: String, target: Path, executionId: String) {
    val response = post {
        url("$baseUrl/test-suites-sources/download-snapshot-by-execution-id?executionId=$executionId")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        onDownload { bytesSentTotal, contentLength ->
            logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
        }
    }
    if (!response.status.isSuccess()) {
        error("Error while downloading test resources: ${response.status}")
    }
//        .bodyAsChannel()
    val bytes = response.body<ByteArray>()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $target/archive.zip")
    FileSystem.SYSTEM.createDirectories(target, mustCreate = false)
    FileSystem.SYSTEM.write(
        target.resolve("archive.zip"),
        mustCreate = true,
    ) {
        write(bytes).flush()
    }
    logDebugCustom("Downloaded archive into $target/archive.zip")
    // todo: unzip
}

/*internal suspend fun HttpClient.downloadAdditionalResources(targetDirectory: Path, additionalResourcesAsString: String) {
    additionalResourcesAsString.split().forEach { additionalResourceName ->
        val fileContentBytes = post {
            url("/files/{organizationName}/{projectName}/download")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.OctetStream)
            setBody(FileKey())
        }
            .body<ByteArray>()
        FileSystem.SYSTEM.write(
            targetDirectory.resolve(),
            mustCreate = true,
        ) {
            write(fileContentBytes)
        }
    }
}*/
