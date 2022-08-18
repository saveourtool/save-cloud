package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.extractZipTo
import com.saveourtool.save.agent.utils.logDebugCustom
import com.saveourtool.save.agent.utils.tryMarkAsExecutable
import com.saveourtool.save.agent.utils.unzipIfRequired
import com.saveourtool.save.agent.utils.writeToFile
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.domain.FileKey
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.cinterop.toKString
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.getenv

internal suspend fun HttpClient.downloadTestResources(config: BackendConfig, target: Path, executionId: String) {
    val response = post {
        url("${config.url}${config.testSourceSnapshotEndpoint}?executionId=$executionId")
        /*url {
            host = config.url
            path(config.testSourceSnapshotEndpoint)
            parameters.append("executionId", executionId)
        }*/
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
//        .bodyAsChannel()
    val bytes = response.body<ByteArray>()
    if (bytes.isEmpty()) {
        error( "Not found any tests for execution $executionId")
    }
    val pathToArchive = "archive.zip".toPath()
    if (fs.exists(pathToArchive)) {
        logDebugCustom("Skipping unpacking, because $pathToArchive already exists in $target")
    } else {
        logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
        bytes.writeToFile(pathToArchive)
        logDebugCustom("Downloaded archive into $pathToArchive")
        fs.createDirectories(target, mustCreate = false)
        pathToArchive.extractZipTo(target)
        fs.delete(pathToArchive, mustExist = true)
        logDebugCustom("Extracted archive into $target")
    }
}

internal suspend fun HttpClient.downloadAdditionalResources(
    baseUrl: String,
    targetDirectory: Path,
    additionalResourcesAsString: String,
) {
    val organizationName = getenv("ORGANIZATION_NAME")!!.toKString()
    val projectName = getenv("PROJECT_NAME")!!.toKString()
    FileKey.parseList(additionalResourcesAsString).map { fileKey ->
        val fileContentBytes = post {
            url("$baseUrl/internal/files/$organizationName/$projectName/download")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.OctetStream)
            setBody(fileKey)
        }
            .body<ByteArray>()
        if (fileContentBytes.isEmpty()) {
            error("Couldn't download file $fileKey: content is empty")
        }
        if (fs.exists(targetDirectory / fileKey.name)) {
            logDebugCustom("Skipping saving of ${fileKey.name} into $targetDirectory because file already exists")
            return
        }
        fileContentBytes.writeToFile(
            targetDirectory / fileKey.name
        )
        fileKey to targetDirectory / fileKey.name
    }
        .onEach { (fileKey, pathToFile) ->
            pathToFile.tryMarkAsExecutable()
            logDebugCustom(
                 "Downloaded $fileKey to ${fs.canonicalize(pathToFile)}"
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
