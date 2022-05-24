/**
 * Utility methods for testing web requests
 */

package com.saveourtool.save.backend.utils

import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * @param uri
 * @param data
 * @param assert
 * @return [ResponseSpec] for assertions
 */
fun WebTestClient.postJsonAndAssert(
    uri: String,
    data: Any? = null,
    assert: WebTestClient.ResponseSpec.() -> Unit,
): WebTestClient.ResponseSpec =
        post().uri(uri)
            .contentType(MediaType.APPLICATION_JSON)
            .run {
                data?.let {
                    bodyValue(data)
                }
                    ?: run {
                        this
                    }
            }
            .exchange()
            .apply(assert)
