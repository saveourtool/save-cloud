/**
 * WebClient utilities that are used in preprocessor for download content from backend
 */

package com.saveourtool.save.preprocessor.utils

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.web.reactive.function.BodyInserter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

private val log = LoggerFactory.getLogger(WebClient::class.java)

/**
 * @param body
 * @param uri
 * @param toBody
 * @return result of [toBody] using response on post request after checking status
 */
fun <M, T> WebClient.makePost(
    body: BodyInserter<M, ReactiveHttpOutputMessage>,
    uri: String,
    toBody: (WebClient.ResponseSpec) -> Mono<T>
): Mono<T> = this.makePost(body, uri).let(toBody)

/**
 * @param body
 * @param uri
 * @return response on post request after checking status
 * @throws ResponseStatusException
 */
fun <M> WebClient.makePost(
    body: BodyInserter<M, ReactiveHttpOutputMessage>,
    uri: String,
): WebClient.ResponseSpec = this
    .post()
    .uri(uri)
    .body(body)
    .retrieve()
    .onStatus({status -> status != HttpStatus.OK }) { clientResponse ->
        log.error("Error when making request to $uri: ${clientResponse.statusCode()}")
        throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Upstream request error"
        )
    }
