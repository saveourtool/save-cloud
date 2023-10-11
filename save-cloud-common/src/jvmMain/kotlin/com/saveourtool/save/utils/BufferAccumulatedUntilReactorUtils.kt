/**
 * Utility method `bufferAccumulatedUntil` for working with Reactor publishers
 */

package com.saveourtool.save.utils

import reactor.core.publisher.Flux
import java.util.function.Predicate

typealias BufferedFlux<T> = Flux<List<T>>
typealias BufferPredicate<T> = Predicate<List<T>>

private data class Buffer<T : Any>(
    val bufferNumber: Int,
    val elements: List<T>,
) {
    val currentElement: T
        get() = elements.last()
}

/**
 * Buffers [T] to [List] of [T] till an accumulated buffer fits by [bufferPredicate]
 * The last one can be in buffer which doesn't fit by [bufferPredicate]
 *
 * @param bufferPredicate
 * @return [Flux] with [List] of [T]
 */
fun <T : Any> Flux<T>.bufferAccumulatedUntil(bufferPredicate: BufferPredicate<T>): BufferedFlux<T> = scan(Buffer(0, emptyList<T>())) { currentBuffer, nextElement ->
    if (bufferPredicate.test(currentBuffer.elements)) {
        Buffer(
            bufferNumber = currentBuffer.bufferNumber,
            elements = currentBuffer.elements.plus(nextElement),
        )
    } else {
        Buffer(
            bufferNumber = currentBuffer.bufferNumber.inc(),
            elements = listOf(nextElement),
        )
    }
}
    .skip(1)
    .bufferUntilChanged { it.bufferNumber }
    .map { buffer -> buffer.map { it.currentElement } }