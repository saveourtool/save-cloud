/**
 * This file contains util methods to work with archives
 */

package com.saveourtool.save.utils

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

const val TAR_EXTENSION = ".tar"

/**
 * Extract path as TAR archive to provided directory
 *
 * @param targetPath
 */
@Suppress("NestedBlockDepth")
fun Path.extractTarTo(targetPath: Path) {
    this.inputStream().use { buffIn ->
        TarArchiveInputStream(buffIn).use { archiveIn ->
            generateSequence { archiveIn.nextTarEntry }.forEach { archiveEntry ->
                val extractedPath = targetPath.resolve(archiveEntry.name)
                if (archiveEntry.isDirectory) {
                    Files.createDirectories(extractedPath)
                } else {
                    extractedPath.outputStream().buffered().use {
                        IOUtils.copy(archiveIn, it)
                    }
                }
            }
        }
    }
}

/**
 * Compress path as TAR archive to provided file
 *
 * @param targetPath
 */
@Suppress("NestedBlockDepth")
fun Path.compressAsTarTo(targetPath: Path) {
    targetPath.outputStream().buffered().use { buffOut ->
        TarArchiveOutputStream(buffOut).use { archiveOut ->
            Files.walk(this).forEach { path ->
                val archiveEntry = archiveOut.createArchiveEntry(path, path.relativize(this).toString())
                archiveOut.putArchiveEntry(archiveEntry)
                if (Files.isRegularFile(path)) {
                    path.inputStream().use { IOUtils.copy(it, archiveOut) }
                }
                archiveOut.closeArchiveEntry()
            }
            archiveOut.finish()
        }
    }
}
