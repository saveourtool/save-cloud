@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.utils

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
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

/**
 * @return content of file as [Flux] of [DataBuffer]
 */
fun Path.toDataBufferFlux(): Flux<DataBuffer> = DataBufferUtils.read(this, DefaultDataBufferFactory.sharedInstance, DEFAULT_BUFFER_SIZE)
    .cast(DataBuffer::class.java)

private const val DEFAULT_BUFFER_SIZE = 4096
