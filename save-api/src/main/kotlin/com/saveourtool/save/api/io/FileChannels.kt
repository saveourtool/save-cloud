@file:JvmName("FileChannels")
@file:Suppress(
    "HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE",
    "TOP_LEVEL_ORDER",
)

package com.saveourtool.save.api.io

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.ReaderScope
import io.ktor.utils.io.WriterScope
import io.ktor.utils.io.core.use
import io.ktor.utils.io.jvm.nio.copyTo
import io.ktor.utils.io.reader
import io.ktor.utils.io.writer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Files.newByteChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.fileSize
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope

private typealias ChannelReader = suspend (ReadableByteChannel) -> Unit

private typealias ChannelWriter = suspend (WritableByteChannel) -> Long

private suspend fun Path.useChannelReader(
    start: Long = 0L,
    channelReader: ChannelReader
) {
    require(start >= 0L) {
        "start position shouldn't be negative but it is $start"
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    newByteChannel(this, READ).use { channel ->
        if (start > 0L) {
            channel.position(start)
        }

        channelReader(channel)
    }
}

private suspend fun Path.useChannelWriter(
    channelWriter: ChannelWriter
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    newByteChannel(this, CREATE, WRITE, TRUNCATE_EXISTING).use { channel ->
        val bytesWritten = channelWriter(channel)
        /*
         * Truncate the tail that could remain from the previously written data.
         */
        channel.truncate(bytesWritten)
    }
}

private suspend fun Path.copyTo(
    target: ByteWriteChannel,
    start: Long = 0L,
    end: Long = fileSize()
) {
    val fileSize = fileSize()
    require(end <= fileSize) {
        "end points to the position out of the file: file size = $fileSize, end = $end"
    }

    useChannelReader(start) { readChannel: ReadableByteChannel ->
        var position = start
        target.writeWhile { buffer ->
            val fileRemaining = end - position
            val bytesRead = when {
                fileRemaining < buffer.remaining() -> {
                    val bufferLimit = buffer.limit()
                    buffer.limit(buffer.position() + fileRemaining.toInt())
                    val bytesRead0 = readChannel.read(buffer)
                    buffer.limit(bufferLimit)
                    bytesRead0
                }

                else -> readChannel.read(buffer)
            }

            if (bytesRead > 0L) {
                position += bytesRead
            }

            bytesRead != -1 && position < end
        }
    }
}

private suspend fun ByteReadChannel.copyTo(target: Path) =
        target.useChannelWriter { writeChannel: WritableByteChannel ->
            copyTo(writeChannel)
        }

private suspend fun Path.copyTo(
    target: WriterScope,
    start: Long = 0L,
    end: Long = fileSize()
) =
        copyTo(target.channel, start, end)

private suspend fun ReaderScope.copyTo(target: Path) =
        channel.copyTo(target)

/**
 * Launches a coroutine to open a read-channel for a file and fill it.
 *
 * Please note that file reading is blocking so if you are starting it on
 * [Dispatchers.Unconfined] it may block your async code and freeze the whole
 * application when runs on a pool that is not intended for blocking operations.
 * This is why [ioContext] should have [Dispatchers.IO] or
 * a coroutine dispatcher that is properly configured for blocking IO.
 *
 * @param start the offset within the file at which reading should start,
 *   defaults to `0L`.
 * @param end the offset (exclusive) at which reading should end; shouldn't
 *   exceed the size of the file.
 *   The default is the size of the file.
 * @param ioContext the context to be used for I/O, defaults to [Dispatchers.IO].
 * @return the newly-opened read-channel.
 */
internal fun Path.readChannel(
    start: Long = 0L,
    end: Long = fileSize(),
    ioContext: CoroutineContext = Dispatchers.IO
): ByteReadChannel {
    val block: suspend (WriterScope) -> Unit = { writerScope ->
        copyTo(writerScope, start, end)
    }

    return CoroutineScope(ioContext).writer(
        coroutineContext = ioContext + CoroutineName("file-reader"),
        autoFlush = false,
        block = block
    ).channel
}

/**
 * Opens a write-channel for the file and launches a coroutine to read from it.
 *
 * Please note that file writing is blocking so if you are starting it on
 * [Dispatchers.Unconfined] it may block your async code and freeze the whole
 * application when runs on a pool that is not intended for blocking operations.
 * This is why [ioContext] should have [Dispatchers.IO] or
 * a coroutine dispatcher that is properly configured for blocking IO.
 *
 * @param ioContext the context to be used for I/O, defaults to [Dispatchers.IO].
 * @return the newly-opened write-channel.
 */
@OptIn(DelicateCoroutinesApi::class)
fun Path.writeChannel(
    ioContext: CoroutineContext = Dispatchers.IO
): ByteWriteChannel {
    val block: suspend (ReaderScope) -> Unit = { readerScope ->
        readerScope.copyTo(this)
    }

    return GlobalScope.reader(
        coroutineContext = ioContext + CoroutineName(name = "file-writer"),
        autoFlush = true,
        block = block
    ).channel
}
