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

fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A, B) -> R): Stream<Pair<R, B>> =
        map { (left, right) ->
            transformLeft(left, right) to right
        }

fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapLeft(transformLeft: (A) -> R): Stream<Pair<R, B>> =
        mapLeft { left, _ ->
            transformLeft(left)
        }

fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (A, B) -> R): Stream<Pair<A, R>> =
        map { (first, second) ->
            first to transformRight(first, second)
        }

fun <A : Any, B : Any, R : Any> Stream<Pair<A, B>>.mapRight(transformRight: (B) -> R): Stream<Pair<A, R>> =
        mapRight { _, right ->
            transformRight(right)
        }
