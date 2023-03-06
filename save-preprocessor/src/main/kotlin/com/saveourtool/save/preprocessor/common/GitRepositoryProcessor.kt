package com.saveourtool.save.preprocessor.common

import org.reactivestreams.Publisher

/**
 * Asynchronously processes a local _Git_ repository.
 */
fun interface GitRepositoryProcessor<T : Publisher<*>> {
    /**
     * Processes the cloned _Git_ repository, returning a `Mono` or a `Flux`.
     *
     * @param cloneResult the local directory along with _Git_ metadata.
     * @return the result of the processing as a custom [Publisher].
     */
    fun processAsync(cloneResult: CloneResult): T
}
