package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.extractZipTo
import com.saveourtool.save.agent.utils.logDebugCustom
import com.saveourtool.save.agent.utils.writeToFile
import com.saveourtool.save.domain.FileKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal suspend fun HttpClient.downloadTestResources(baseUrl: String, target: Path, executionId: String) {
    val response = post {
        url("$baseUrl/internal/test-suites-sources/download-snapshot-by-execution-id?executionId=$executionId")
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
    if (bytes.isEmpty()) {
        error( "Not found any tests for execution $executionId")
    }
    val pathToArchive = "archive.zip".toPath()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
    bytes.writeToFile(pathToArchive)
    logDebugCustom("Downloaded archive into $pathToArchive")
    fs.createDirectories(target, mustCreate = false)
    pathToArchive.extractZipTo(target)
    fs.delete(pathToArchive, mustExist = true)
    logDebugCustom("Extracted archive into $target")
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
        fs.write(
            targetDirectory.resolve(),
            mustCreate = true,
        ) {
            write(fileContentBytes)
        }
    }
}*/
