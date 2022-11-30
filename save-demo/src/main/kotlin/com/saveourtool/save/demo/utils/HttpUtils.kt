@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.demo.utils

import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

private const val DEFAULT_BUFFER_SIZE = 4096

/**
 * Read bytes from [ByteReadChannel] as [Flux] of [ByteBuffer].
 * The input stream is closed when Flux is terminated.
 *
 * @return [Flux] of [ByteBuffer]s read from [ByteReadChannel]
 */
fun ByteReadChannel.toByteBufferFlux(): Flux<ByteBuffer> = DataBufferUtils.readInputStream(
    ::toInputStream,
    DefaultDataBufferFactory.sharedInstance,
    DEFAULT_BUFFER_SIZE,
).map { it.asByteBuffer() }
