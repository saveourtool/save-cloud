/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.core.files.findAllFilesMatching
import com.saveourtool.save.utils.extractZipTo
import com.saveourtool.save.utils.fs
import com.saveourtool.save.utils.markAsExecutable
import okio.FileNotFoundException
import okio.Path
import okio.Path.Companion.toPath

/**
 * Read file as a list of strings
 *
 * @param filePath a file to read
 * @return list of string from file
 */
internal fun readFile(filePath: String): List<String> = try {
    val path = filePath.toPath()
    fs.read(path) {
        generateSequence { readUtf8Line() }.toList()
    }
} catch (e: FileNotFoundException) {
    logErrorCustom("Not able to find file in the following path: $filePath")
    emptyList()
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
