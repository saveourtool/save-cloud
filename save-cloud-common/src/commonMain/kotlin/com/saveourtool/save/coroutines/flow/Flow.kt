@file:JvmName("Flow")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.coroutines.flow

import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Decodes this byte flow into a flow of strings, assuming UTF-8 encoding.
 * Strings are emitted each time a newline (`\n`) is encountered in the byte
 * flow, or when the flow completes.
 *
 * Malformed byte sequences are replaced with `\uFFFD`.
 *
 * @see ByteArray.decodeToString
 */
fun Flow<Byte>.decodeToString(): Flow<String> =
        flow {
            var accumulator: MutableList<Byte> = arrayListOf()

            collect { value ->
                accumulator = when (value) {
                    /*
                     * Ignore.
                     */
                    '\r'.code.toByte() -> accumulator

                    '\n'.code.toByte() -> {
                        emit(accumulator)
                        arrayListOf()
                    }

                    else -> accumulator.apply {
                        add(value)
                    }
                }
            }

            emit(accumulator)
        }
            .map(Collection<Byte>::toByteArray)
            .map(ByteArray::decodeToString)
