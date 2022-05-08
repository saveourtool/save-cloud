/**
 * Utilities for working with Linux
 */

package org.cqfn.save.orchestrator

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * @return IP address of the docker host or `host-gateway` as a fallback
 */
fun getHostIp(): String {
    System.getenv("HOST_IP")?.let {
        return it
    }
    return resolve("host.docker.internal")
        ?: "host-gateway"
}

/**
 * Copy [sourceDir] into [targetDir] recursively, while also copying original file attributes
 *
 * @param sourceDir source directory
 * @param targetDir target directory
 * @throws FileNotFoundException if source dir doesn't exists
 */
fun copyRecursivelyWithAttributes(sourceDir: File, targetDir: File) {
    if (!sourceDir.exists()) {
        throw FileNotFoundException("Source directory $sourceDir doesn't exist!")
    }
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

/**
 * @param hostname hostname to be resolved via `hosts` file
 * @return IP address of [hostname] if it has been found or null
 */
private fun resolve(hostname: String): String? {
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
