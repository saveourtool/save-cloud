@file:JvmName("StreamUtils")
@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save.backend.utils

import java.util.Spliterator.ORDERED
import java.util.Spliterator.SIZED
import java.util.Spliterators.AbstractSpliterator
import java.util.Spliterators.iterator
import java.util.function.Consumer
import java.util.stream.Stream
import java.util.stream.StreamSupport.stream
import kotlin.math.min

/**
 * Returns a stream of pairs built from the elements of `this` stream and the
 * [other] stream with the same index.
 * The resulting stream ends as soon as the shortest input stream ends.
 *
 * The operation is _intermediate_ and _stateless_.
 *
 * @param T the type of elements in `this` stream.
 * @param R the type of elements in the [other] stream.
 * @param other the other stream to zip with this one.
 * @return the stream of pairs built from the elements of `this` stream and the
 *   [other] stream.
 * @see Sequence.zip
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG")
infix fun <T, R> Stream<T>.zip(other: Stream<R>): Stream<Pair<T, R>> {
    val isParallel = isParallel || other.isParallel
    val splitrLeft = spliterator()
    val splitrRight = other.spliterator()
    val characteristics = splitrLeft.characteristics() and
            splitrRight.characteristics() and
            (SIZED or ORDERED)
    val itrLeft = iterator(splitrLeft)
    val itrRight = iterator(splitrRight)

    return stream(
        object : AbstractSpliterator<Pair<T, R>>(
            min(splitrLeft.estimateSize(), splitrRight.estimateSize()),
            characteristics
        ) {
            override fun tryAdvance(action: Consumer<in Pair<T, R>>): Boolean =
                    when {
                        itrLeft.hasNext() && itrRight.hasNext() -> {
                            action.accept(itrLeft.next() to itrRight.next())
                            true
                        }

                        else -> false
                    }
        },
        isParallel,
    )
        .onClose(this::close)
        .onClose(other::close)
}

/**
 * Transforms the [left][Pair.first] value of each element of this [Stream].
 *
 * @param transformLeft the mapper function.
 * @return the transformed [Stream].
 * @see Stream.map
 */
fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A, B) -> R): Stream<Pair<R, B>> =
        map { (left, right) ->
            transformLeft(left, right) to right
        }

/**
 * Transforms the [left][Pair.first] value of each element of this [Stream].
 *
 * @param transformLeft the mapper function.
 * @return the transformed [Stream].
 * @see Stream.map
 */
fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A) -> R): Stream<Pair<R, B>> =
        mapLeft { left, _ ->
            transformLeft(left)
        }

/**
 * Transforms the [right][Pair.second] value of each element of this [Stream].
 *
 * @param transformRight the mapper function.
 * @return the transformed [Stream].
 * @see Stream.map
 */
fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (A, B) -> R): Stream<Pair<A, R>> =
        map { (first, second) ->
            first to transformRight(first, second)
        }

/**
 * Transforms the [right][Pair.second] value of each element of this [Stream].
 *
 * @param transformRight the mapper function.
 * @return the transformed [Stream].
 * @see Stream.map
 */
fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (B) -> R): Stream<Pair<A, R>> =
        mapRight { _, right ->
            transformRight(right)
        }
