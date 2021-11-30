/**
 * Utilities for working with Linux
 */

package org.cqfn.save.orchestrator

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @param hostname hostname to be resolved via `hosts` file
 * @return IP address of [hostname] if it has been found or null
 */
fun getHostIp(hostname: String): String? {
    val process = ProcessBuilder(
        "bash", "-c",
        "getent hosts $hostname | awk '{print \$1}'"
    )
        .start()
    process.waitFor()
    return process.inputStream
        .readAllBytes()
        .decodeToString()
        .lines()
        .firstOrNull()
        ?.takeIf { it.isNotBlank() }
}

/**
 * Copy [sourceDir] into [targetDir] recursively, while also copying original file attributes
 *
 * @param sourceDir source directory
 * @param targetDir target directory
 */
fun copyRecursivelyWithAttributes(sourceDir: File, targetDir: File) {
    sourceDir.walkTopDown().forEach { source ->
        val target = targetDir.resolve(source.relativeTo(sourceDir)).canonicalFile
        if (source.isDirectory) {
            target.mkdirs()
        }
        Files.copy(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.COPY_ATTRIBUTES,
        )
    }
}
