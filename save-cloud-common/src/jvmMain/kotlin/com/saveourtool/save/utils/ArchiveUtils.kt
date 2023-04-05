/**
 * This file contains util methods to work with archives
 */

package com.saveourtool.save.utils

import okio.Path
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.examples.Archiver
import org.apache.commons.compress.archivers.examples.Expander
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.io.path.absolutePathString

const val ARCHIVE_EXTENSION = ".${ArchiveStreamFactory.ZIP}"

private val log: Logger = LoggerFactory.getLogger(object {}.javaClass.enclosingClass::class.java)
private val archiver = Archiver()
private val expander = Expander()

/**
 * Extract path as ZIP archive to provided directory
 *
 * @param targetPath
 */
actual fun Path.extractZipTo(targetPath: Path) {
    log.debug { "Unzip $this into $targetPath" }
    expander.expand(ArchiveStreamFactory.ZIP, toFile(), targetPath.toFile())
}

/**
 * Extract path as ZIP archive to parent
 */
actual fun Path.extractZipHere() = parent?.let {
    extractZipTo(it)
} ?: throw FileSystemException(this.toFile(), null, "Path to archive is set incorrectly.")

/**
 * Compress path as ZIP archive to provided file
 *
 * @param targetPath
 */
@Suppress("NestedBlockDepth")
fun java.nio.file.Path.compressAsZipTo(targetPath: java.nio.file.Path) {
    log.debug { "Zip ${absolutePathString()} into ${targetPath.absolutePathString()}" }
    archiver.create(ArchiveStreamFactory.ZIP, targetPath, this)
}

/**
 * Compress path as ZIP archive to provided file
 *
 * @param targetPath
 */
fun Path.compressAsZipTo(targetPath: Path) = toNioPath().compressAsZipTo(targetPath.toNioPath())
