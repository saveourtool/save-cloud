package com.saveourtool.save.spring.utils

import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.WebClient

fun WebClient.Builder.applyAll(customizers: Iterable<WebClientCustomizer>): WebClient.Builder = apply {
    customizers.forEach {
        it.customize(this)
    }
}
