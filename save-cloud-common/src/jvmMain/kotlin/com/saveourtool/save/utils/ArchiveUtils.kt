/**
 * This file contains util methods to work with archives
 */

package com.saveourtool.save.utils

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.examples.Archiver
import org.apache.commons.compress.archivers.examples.Expander
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
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
fun Path.extractZipTo(targetPath: Path) {
    log.debug { "Unzip ${absolutePathString()} into ${targetPath.absolutePathString()}" }
    expander.expand(ArchiveStreamFactory.ZIP, toFile(), targetPath.toFile())
}

/**
 * Extract path as ZIP archive to parent
 */
fun Path.extractZipHere() {
    extractZipTo(parent)
}

/**
 * Compress path as ZIP archive to provided file
 *
 * @param targetPath
 */
@Suppress("NestedBlockDepth")
fun Path.compressAsZipTo(targetPath: Path) {
    log.debug { "Zip ${absolutePathString()} into ${targetPath.absolutePathString()}" }
    archiver.create(ArchiveStreamFactory.ZIP, targetPath, this)
}
