package org.cqfn.save.backend.utils

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.Optional

fun <T> justOrNotFound(data: Optional<T>, message: String? = null) = Mono.justOrEmpty(data)
    .switchIfEmpty {
        Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, message))
    }
