/**
 * This file contains util methods to work with archives
 */

package com.saveourtool.save.utils

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.examples.Archiver
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.utils.IOUtils
import java.nio.file.*
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

const val ARCHIVE_EXTENSION = ".${ArchiveStreamFactory.ZIP}"

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
fun Path.compressAsZipTo(targetPath: Path) {
    Archiver().create(ArchiveStreamFactory.ZIP, targetPath, this)
}
