/**
 * Utility methods to work with files using Okio
 */

package org.cqfn.save.agent.utils

import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.zlib.*

import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value

/**
 * @param s
 * @return
 */
fun deflate(s: String): ByteArray = memScoped {
    // val defstream: z_stream = z_stream()
    // defstream.zalloc = Z_NULL
    // defstream.zfree = Z_NULL
    // defstream.avail_in = s.length.toUInt()  // size of input
    // defstream.next_in = UByteVarOf<UByte>(s.cstr.ptr.rawValue).ptr
    // defstream.avail_in = s.length.toUInt()  // size of input
    val out = allocArray<UByteVar>(s.length)
    // defstream.next_out = UByteVarOf<UByte>(out.rawValue).ptr
    // deflateInit(defstream.ptr, Z_BEST_COMPRESSION)
    // platform.zlib.deflate(defstream.ptr, Z_FINISH)
    // deflateEnd(defstream.ptr)
    val destLen = cValuesOf(s.length).ptr
    compress(
        out,
        destLen.reinterpret(),
        s.cstr.ptr.reinterpret(),
        s.length.toULong()
    )
    return@memScoped out.readBytes(destLen.pointed.value)
}

/**
 * Read file as a list of strings
 *
 * @param filePath a file to read
 * @return list of string from file
 */
internal fun readFile(filePath: String): List<String> = try {
    val path = filePath.toPath()
    FileSystem.SYSTEM.read(path) {
        generateSequence { readUtf8Line() }.toList()
    }
} catch (e: FileNotFoundException) {
    logErrorCustom("Not able to find file in the following path: $filePath")
    emptyList()
}

/**
 * Read properties file as a map
 *
 * @param filePath a file to read
 * @return map of properties with values
 */
internal fun readProperties(filePath: String): Map<String, String> = readFile(filePath)
    .associate { line ->
        line.split("=").map { it.trim() }.let {
            require(it.size == 2)
            it.first() to it.last()
        }
    }
