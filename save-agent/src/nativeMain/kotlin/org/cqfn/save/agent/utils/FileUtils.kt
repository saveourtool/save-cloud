/**
 * Utility methods to work with files using Okio
 */

package org.cqfn.save.agent.utils

import okio.FileNotFoundException
import okio.FileSystem
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
    println("Not able to find file in the following path: $filePath")
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
