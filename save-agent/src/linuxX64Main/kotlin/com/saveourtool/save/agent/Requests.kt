/**
 * Utilities to perform requests to other services of save-cloud
 */

package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.*
import com.saveourtool.save.agent.utils.extractZipTo
import com.saveourtool.save.agent.utils.markAsExecutable
import com.saveourtool.save.agent.utils.unzipIfRequired
import com.saveourtool.save.agent.utils.writeToFile
import com.saveourtool.save.core.logging.logWarn
import com.saveourtool.save.core.utils.runIf
import com.saveourtool.save.domain.FileKey

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
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
    val result = processRequestToBackendWrapped {
        httpClient.downloadTestResources(config, executionId)
    }
    if (result.failureOrNotOk()) {
        throw IllegalStateException("Couldn't download test resources")
    }

    val bytes = result.getOrThrow()
        .readByteArrayOrThrowIfEmpty {
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
 * Download additional resources from [additionalFiles] into [targetDirectory]
 *
 * @param config
 * @param targetDirectory
 * @param additionalFiles
 * @return result
 */
internal suspend fun SaveAgent.downloadAdditionalResources(
    config: BackendConfig,
    targetDirectory: Path,
    additionalFiles: List<FileKey>,
) = runCatching {
    additionalFiles
        .map { fileKey ->
            val result = processRequestToBackendWrapped {
                httpClient.download(
                    url = "${config.url}${config.fileEndpoint}",
                    body = fileKey,
                )
            }
            if (result.failureOrNotOk()) {
                throw IllegalStateException("Couldn't download file $fileKey")
            }

            val fileContentBytes = result.getOrThrow()
                .readByteArrayOrThrowIfEmpty {
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

/**
 * Downloads binary of save-cli into the current directory
 *
 * @param url
 * @throws IllegalStateException
 */
internal suspend fun SaveAgent.downloadSaveCli(url: String) {
    val result = processRequestToBackendWrapped {
        httpClient.download(
            url = url,
            body = null,
        )
    }
    if (result.failureOrNotOk()) {
        throw IllegalStateException("Couldn't download save-cli")
    }

    val bytes = result.getOrThrow()
        .readByteArrayOrThrowIfEmpty {
            error("Downloaded file is empty")
        }
    bytes.writeToFile(SAVE_CLI_EXECUTABLE_NAME.toPath())
}

private suspend fun HttpClient.downloadTestResources(config: BackendConfig, executionId: String) = download(
    url = "${config.url}${config.testSourceSnapshotEndpoint}?executionId=$executionId",
    body = null,
)

private suspend fun HttpResponse.readByteArrayOrThrowIfEmpty(exceptionSupplier: ByteArray.() -> Nothing) =
        body<ByteArray>().runIf({ isEmpty() }, exceptionSupplier)
