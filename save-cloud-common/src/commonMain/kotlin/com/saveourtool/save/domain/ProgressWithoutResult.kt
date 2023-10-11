package com.saveourtool.save.domain

import kotlinx.serialization.Serializable

interface Progressable {
    val progress: Long
}

/**
 * A base progress interface
 *
 * @property progress current progress from 0 to 100
 * @property result
 */
@Serializable
data class Progress<T>(
    override val progress: Long,
    val result: T,
): Progressable {
    companion object {
        /**
         * @return [Progress] without result
         */
        operator fun invoke(progress: Long): Progress<Unit> = Progress(progress, Unit)

        /**
         * @return [Progress] with [this] as result
         */
        fun <T> T.withProgress(progress: Long): Progress<T> = Progress(progress, this)
    }
}
