package com.saveourtool.save.demo.utils

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Tests for [ByteReadChannel.toByteBufferFlux].
 *
 * @see ByteReadChannel.toByteBufferFlux
 */
class HttpUtilsTest {
    @Test
    fun `empty channel`() {
        val emptyChannel: ByteReadChannel = ByteReadChannel.Empty

        val byteBuffersOrNull: List<ByteBuffer>? = emptyChannel.toByteBufferFlux().collectList().block()
        assertThat(byteBuffersOrNull).isNotNull.isEmpty()
    }

    @Test
    fun `closed channel`() {
        val closedStream = ByteArrayInputStream(byteArrayOf()).apply {
            close()
        }

        assertThat(closedStream.read()).isEqualTo(-1)

        val closedChannel = closedStream.toByteReadChannel()

        val byteBuffersOrNull: List<ByteBuffer>? = closedChannel.toByteBufferFlux().collectList().block()
        assertThat(byteBuffersOrNull).isNotNull.isEmpty()
    }

    @Test
    fun `exception thrown when blocking`() {
        val errorMessage = "Message"

        val faultyChannel = object : InputStream() {
            override fun read() =
                    throw IOException(errorMessage)
        }.toByteReadChannel()

        val buffersOrNone: Mono<List<ByteBuffer>> = faultyChannel.toByteBufferFlux().collectList()

        assertThatThrownBy {
            buffersOrNone.block()
        }.isInstanceOf(RuntimeException::class.java)
            .hasMessage("${IOException::class.qualifiedName}: $errorMessage")
            .hasCauseExactlyInstanceOf(IOException::class.java)
    }
}
