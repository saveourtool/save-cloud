/**
 * Utility methods to work with files using Okio
 */

package com.saveourtool.save.agent.utils

import com.saveourtool.save.agent.fs
import okio.FileNotFoundException
import okio.FileSystem
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
    FileSystem.SYSTEM.read(path) {
        generateSequence { readUtf8Line() }.toList()
    }
} catch (e: FileNotFoundException) {
    logErrorCustom("Not able to find file in the following path: $filePath")
    emptyList()
}

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
 * Extract path as ZIP archive to provided directory
 *
 * @param targetPath
 */
internal fun Path.extractZipTo(targetPath: Path) {
    require(fs.metadata(targetPath).isDirectory)
    logDebugCustom("Unzip ${fs.canonicalize(this)} into ${fs.canonicalize(targetPath)}")
    platform.posix.system("unzip $this -d $targetPath")
}

internal fun ByteArray.writeToFile(file: Path, mustCreate: Boolean = true) {
    fs.write(
        file = file,
        mustCreate = mustCreate,
    ) {
        write(this@writeToFile).flush()
    }
}

internal fun Path.tryMarkAsExecutable() {
    platform.posix.chmod(
        this.toString(),
        755,
    )
}

internal fun unzipIfRequired(
    pathToFile: Path,
) {
    // FixMe: for now support only .zip files
    if (pathToFile.name.endsWith(".zip")) {
//        val shouldBeExecutable = Files.getPosixFilePermissions(pathToFile).any { allExecute.contains(it) }
        pathToFile.extractZipTo(pathToFile.parent!!)
//        if (shouldBeExecutable) {
//            log.info { "Marking files in ${pathToFile.parent} executable..." }
//            Files.walk(pathToFile.parent)
//                .filter { it.isRegularFile() }
//                .forEach { with(loggingContext) { it.tryMarkAsExecutable() } }
//        }
        fs.delete(pathToFile, mustExist = true)
    }
}
