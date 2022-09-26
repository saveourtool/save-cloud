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
    val pathToArchive = "archive.zip".toPath()
    download("tests", url, pathToArchive)
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
    logDebugCustom("Will now download additional resources from $additionalFileToUrl")
    additionalFileToUrl
        .map { (fileName, url) ->
            val targetFile = targetDirectory / fileName
            download("additional file $fileName", url, targetFile)
            targetFile.markAsExecutable()
            unzipIfRequired(targetFile)
        }
        .ifEmpty {
            logWarn("Not found any additional files")
            emptyList()
        }
    logInfoCustom("Downloaded all additional resources to $targetDirectory")
}

/**
 * Downloads binary of save-cli into the current directory
 *
 * @param url
 * @throws IllegalStateException
 */
internal suspend fun SaveAgent.downloadSaveCli(url: String) {
    download("save-cli", url, SAVE_CLI_EXECUTABLE_NAME.toPath())
    SAVE_CLI_EXECUTABLE_NAME.toPath().markAsExecutable()
}

private suspend fun SaveAgent.download(fileLabel: String, url: String, target: Path) {
    logDebugCustom("Will now download $fileLabel from $url into $target")
    val result = processRequestToBackendWrapped {
        httpClient.download(
            url = url,
            body = null,
        )
    }
    if (result.failureOrNotOk()) {
        throw IllegalStateException("Couldn't download $fileLabel from $url")
    }

    val bytes = result.getOrThrow()
        .readByteArrayOrThrowIfEmpty {
            error("Downloaded $fileLabel from $url but content is empty")
        }
    bytes.writeToFile(target)
    logInfoCustom("Downloaded $fileLabel (bytes = ${bytes.size}) from $url into $target")
}

private suspend fun HttpResponse.readByteArrayOrThrowIfEmpty(exceptionSupplier: ByteArray.() -> Nothing) =
        body<ByteArray>().runIf({ isEmpty() }, exceptionSupplier)
