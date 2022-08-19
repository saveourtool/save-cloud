/**
 * Utilities to perform requests to other services of save-cloud
 */

package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.*
import com.saveourtool.save.agent.utils.extractZipTo
import com.saveourtool.save.agent.utils.markAsExecutable
import com.saveourtool.save.agent.utils.requiredEnv
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

/**
 * Download test source snapshots for execution [executionId] into [target]
 *
 * @param config
 * @param target
 * @param executionId
 * @return result
 */
internal suspend fun SaveAgent.downloadTestResources(config: BackendConfig, target: Path, executionId: String): Result<Unit> = runCatching {
    val result = httpClient.downloadTestResources(config, executionId)
    if (updateState(result)) {
        return@runCatching
    }

    val response = result.getOrThrow()
    val bytes = response.body<ByteArray>().runIf({ isEmpty() }) {
        error("Not found any tests for execution $executionId")
    }
    val pathToArchive = "archive.zip".toPath()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
    bytes.writeToFile(pathToArchive)
    fs.createDirectories(target, mustCreate = false)
    pathToArchive.extractZipTo(target)
    fs.delete(pathToArchive, mustExist = true)
    logDebugCustom("Extracted archive into $target and deleted $pathToArchive")
}

/**
 * Download additional resources from [additionalResourcesAsString] into [targetDirectory]
 *
 * @param baseUrl
 * @param targetDirectory
 * @param additionalResourcesAsString
 * @return result
 */
internal suspend fun SaveAgent.downloadAdditionalResources(
    baseUrl: String,
    targetDirectory: Path,
    additionalResourcesAsString: String,
) = runCatching {
    val organizationName = requiredEnv("ORGANIZATION_NAME")
    val projectName = requiredEnv("PROJECT_NAME")
    FileKey.parseList(additionalResourcesAsString)
        .map { fileKey ->
            val result = httpClient.downloadFile(
                "$baseUrl/internal/files/$organizationName/$projectName/download",
                fileKey
            )
            if (updateState(result)) {
                return@runCatching
            }

            val fileContentBytes = result.getOrThrow()
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

private suspend fun HttpClient.downloadTestResources(config: BackendConfig, executionId: String) = runCatching {
    post {
        url("${config.url}${config.testSourceSnapshotEndpoint}?executionId=$executionId")
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        onDownload { bytesSentTotal, contentLength ->
            logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
        }
    }
}

private suspend fun HttpClient.downloadFile(url: String, fileKey: FileKey): Result<HttpResponse> = runCatching {
    post {
        url(url)
        contentType(ContentType.Application.Json)
        accept(ContentType.Application.OctetStream)
        setBody(fileKey)
        onDownload { bytesSentTotal, contentLength ->
            logDebugCustom("Received $bytesSentTotal bytes from $contentLength")
        }
    }
}
