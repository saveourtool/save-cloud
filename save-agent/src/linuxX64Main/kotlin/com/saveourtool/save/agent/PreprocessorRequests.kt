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
    val pathToArchive = "archive.zip".toPath()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
    FileSystem.SYSTEM.createDirectories(target, mustCreate = false)
    FileSystem.SYSTEM.write(
        "archive.zip".toPath(),
        mustCreate = true,
    ) {
        write(bytes).flush()
    }
    logDebugCustom("Downloaded archive into $pathToArchive")
    platform.posix.system("unzip $pathToArchive -d $target")
    FileSystem.SYSTEM.delete(pathToArchive, mustExist = true)
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
        FileSystem.SYSTEM.write(
            targetDirectory.resolve(),
            mustCreate = true,
        ) {
            write(fileContentBytes)
        }
    }
}*/
