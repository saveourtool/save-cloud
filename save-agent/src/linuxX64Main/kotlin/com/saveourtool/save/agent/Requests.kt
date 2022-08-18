package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.*
import com.saveourtool.save.agent.utils.extractZipTo
import com.saveourtool.save.agent.utils.requiredEnv
import com.saveourtool.save.agent.utils.markAsExecutable
import com.saveourtool.save.agent.utils.unzipIfRequired
import com.saveourtool.save.agent.utils.writeToFile
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.domain.FileKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.Path
import okio.Path.Companion.toPath

internal suspend fun HttpClient.downloadTestResources(config: BackendConfig, target: Path, executionId: String) {
    val response = post {
        url("${config.url}${config.testSourceSnapshotEndpoint}?executionId=$executionId")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        onDownload { bytesSentTotal, contentLength ->
            logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
        }
    }
    if (!response.status.isSuccess()) {
        logDebugCustom("Error during request to ${response.request.url}: ${response.status}")
        error("Error while downloading test resources: ${response.status}")
    }

    val bytes = response.body<ByteArray>().runIf({ isEmpty() }) {
        error( "Not found any tests for execution $executionId")
    }
    val pathToArchive = "archive.zip".toPath()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
    bytes.writeToFile(pathToArchive)
    fs.createDirectories(target, mustCreate = false)
    pathToArchive.extractZipTo(target)
    fs.delete(pathToArchive, mustExist = true)
    logDebugCustom("Extracted archive into $target and deleted $pathToArchive")
}

internal suspend fun HttpClient.downloadAdditionalResources(
    baseUrl: String,
    targetDirectory: Path,
    additionalResourcesAsString: String,
) {
    val organizationName = requiredEnv("ORGANIZATION_NAME")
    val projectName = requiredEnv("PROJECT_NAME")
    FileKey.parseList(additionalResourcesAsString).map { fileKey ->
        val fileContentBytes = post {
            url("$baseUrl/internal/files/$organizationName/$projectName/download")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.OctetStream)
            setBody(fileKey)
            onDownload { bytesSentTotal, contentLength ->
                logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
            }
        }
            .body<ByteArray>()
            .runIf({ isEmpty() }) {
                error("Couldn't download file $fileKey: content is empty")
            }

        val targetFile = targetDirectory / fileKey.name
        fileContentBytes.writeToFile(targetFile)
        fileKey to targetFile
    }
        .onEach { (fileKey, pathToFile) ->
            pathToFile.markAsExecutable()
            logDebugCustom(
                 "Downloaded $fileKey into ${fs.canonicalize(pathToFile)}"
            )
        }
        .map { (_, pathToFile) ->
            unzipIfRequired(pathToFile)
        }
        .ifEmpty {
            logWarn("Not found any additional files for execution \$id")
            emptyList()
        }
}
