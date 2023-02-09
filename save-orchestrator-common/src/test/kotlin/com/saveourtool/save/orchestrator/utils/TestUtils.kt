/**
 * Test utilities for orchestrator
 */

package com.saveourtool.save.orchestrator.utils

import com.github.dockerjava.api.command.SyncDockerCmd
import com.github.dockerjava.api.exception.DockerException
import com.saveourtool.save.utils.EmptyResponse
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

/**
 * [EmptyResponse] as [Mono]
 */
internal val emptyResponseAsMono: Mono<EmptyResponse> = Mono.just(ResponseEntity.ok().build())

/**
 * @return [R] result of [C] or null, [DockerException] is ignored
 */
internal fun <R, C : SyncDockerCmd<R>> C.execIgnoringException(): R? = try {
    this.exec()
} catch (_: DockerException) {
    // ignoring exception
    null
}
