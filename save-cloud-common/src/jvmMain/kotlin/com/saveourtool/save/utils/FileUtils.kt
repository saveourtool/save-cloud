@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.utils

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Move [source] into [destinationDir], while also copying original file attributes
 *
 * @param source source file
 * @param destinationDir destination directory
 * @throws FileNotFoundException if source doesn't exists
 */
fun moveFileWithAttributes(source: File, destinationDir: File) {
    if (!source.exists()) {
        throw FileNotFoundException("Source file $source doesn't exist!")
    }

    Files.copy(source.toPath(), destinationDir.resolve(source.name).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
    Files.delete(source.toPath())
}
