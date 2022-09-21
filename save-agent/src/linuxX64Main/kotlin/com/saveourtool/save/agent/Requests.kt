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
import generated.SAVE_CORE_VERSION

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import okio.Path
import okio.Path.Companion.toPath

/**
 * Download test source snapshots from [url] into [target]
 *
 * @param url
 * @param target
 * @return result
 */
internal suspend fun SaveAgent.downloadTestResources(url: String, target: Path): Result<Unit> = runCatching {
    val result = processRequestToBackendWrapped {
        httpClient.download(
            url = url,
            body = null
        )
    }
    if (result.failureOrNotOk()) {
        error("Couldn't download test resources from $url")
    }

    val bytes = result.getOrThrow()
        .readByteArrayOrThrowIfEmpty {
            error("Not found any tests: empty response from $url")
        }
    val pathToArchive = "archive.zip".toPath()
    logDebugCustom("Writing downloaded archive of size ${bytes.size} into $pathToArchive")
    bytes.writeToFile(pathToArchive)
    fs.createDirectories(target, mustCreate = false)
    pathToArchive.extractZipTo(target)
    fs.delete(pathToArchive, mustExist = true)
    logDebugCustom("Extracted archive into $target and deleted $pathToArchive")
    logInfoCustom("Downloaded all tests from $url to $target")
}

/**
 * Download additional resources from urls in values of [additionalFileToUrl] into [targetDirectory]
 *
 * @param targetDirectory
 * @param additionalFileToUrl
 * @return result
 */
internal suspend fun SaveAgent.downloadAdditionalResources(
    targetDirectory: Path,
    additionalFileToUrl: Map<String, String>,
) = runCatching {
    additionalFileToUrl
        .map { (fileName, url) ->
            val result = processRequestToBackendWrapped {
                httpClient.download(
                    url = url,
                    body = null,
                )
            }
            if (result.failureOrNotOk()) {
                error("Couldn't download file $fileName from $url")
            }

            val fileContentBytes = result.getOrThrow()
                .readByteArrayOrThrowIfEmpty {
                    error("Couldn't download file $fileName from $url: content is empty")
                }
            val targetFile = targetDirectory / fileName
            fileContentBytes.writeToFile(targetFile)
            logDebugCustom(
                "Downloaded $fileName from $url into ${fs.canonicalize(targetFile)}"
            )
            targetFile.markAsExecutable()
            unzipIfRequired(targetFile)
        }
        .ifEmpty {
            logWarn("Not found any additional files")
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
    logDebugCustom("Wil now download save-cli from $url")
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
    SAVE_CLI_EXECUTABLE_NAME.toPath().markAsExecutable()
}

private suspend fun HttpClient.downloadTestResources(config: BackendConfig, executionId: String) = download(
    url = "${config.url}${config.testSourceSnapshotEndpoint}?executionId=$executionId",
    body = null,
)

private suspend fun HttpResponse.readByteArrayOrThrowIfEmpty(exceptionSupplier: ByteArray.() -> Nothing) =
        body<ByteArray>().runIf({ isEmpty() }, exceptionSupplier)
