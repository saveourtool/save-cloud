/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.core.files.findAllFilesMatching
import okio.FileSystem
import okio.Path

internal expect val fs: FileSystem

/**
 * Extract path as ZIP archive to provided directory
 *
 * @param targetPath
 */
internal expect fun Path.extractZipTo(targetPath: Path)

/**
 * Write content of [this] into a file [file]
 *
 * @receiver [ByteArray] to be written into a file
 * @param file target [Path]
 * @param mustCreate will be passed to Okio's [FileSystem.write]
 */
internal expect fun ByteArray.writeToFile(file: Path, mustCreate: Boolean = true)

/**
 * Mark [this] file as executable. Sets permissions to rwxr--r--
 */
internal expect fun Path.markAsExecutable()

/**
 * Read file as a list of strings
 *
 * @param filePath a file to read
 * @return list of string from file
 */
internal expect fun readFile(filePath: String): List<String>

/**
 * Read properties file as a map
 *
 * @param filePath a file to read
 * @return map of properties with values
 */
internal fun readProperties(filePath: String): Map<String, String> = readFile(filePath)
    .associate { line ->
        line.split("=").map { it.trim() }.let {
            require(it.size == 2)
            it.first() to it.last()
        }
    }

/**
 * @param pathToFile
 */
internal fun unzipIfRequired(
    pathToFile: Path,
) {
    // FixMe: for now support only .zip files
    if (pathToFile.name.endsWith(".zip")) {
        pathToFile.extractZipTo(pathToFile.parent!!)
        // fixme: need to store information about isExecutable in Execution (FileKey)
        pathToFile.parent!!.findAllFilesMatching {
            if (fs.metadata(it).isRegularFile) {
                it.markAsExecutable()
            }
            true
        }
        fs.delete(pathToFile, mustExist = true)
    }
}
