package org.cqfn.save.backend.controllers

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import reactor.core.publisher.Flux

@Controller
class HelloController {
    @GetMapping(path = ["/numbers"],
        produces = [MediaType.APPLICATION_STREAM_JSON_VALUE])
    @ResponseBody
    fun getNumbers(): Flux<Int> {
        return Flux.range(1, 100)
    }
}