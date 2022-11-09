/**
 * Utilities for spring WebClient
 */

package com.saveourtool.save.spring.utils

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.WebClient

/**
 * Applies all [WebClientCustomizer]s from [customizers] to [this] [WebClient.Builder].
 *
 * @param customizers [WebClientCustomizer]s to be applied
 * @return the modified builder
 */
fun WebClient.Builder.applyAll(customizers: Iterable<WebClientCustomizer>): WebClient.Builder = apply {
    customizers.forEach {
        it.customize(this)
    }
}
