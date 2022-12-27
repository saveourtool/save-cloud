/**
 * Test utilities for orchestrator
 */

package com.saveourtool.save.orchestrator.utils

import com.saveourtool.save.utils.EmptyResponse
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

/**
 * [EmptyResponse] as [Mono]
 */
internal val emptyResponseAsMono: Mono<EmptyResponse> = Mono.just(ResponseEntity.ok().build())
