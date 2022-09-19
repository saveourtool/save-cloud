package com.saveourtool.save.orchestrator.sandbox

import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

typealias EmptyResponse = ResponseEntity<Unit>

@RestController
@RequestMapping("/sandbox/api")
class SandboxController {
    @PostMapping("/upload-files")
    fun uploadFiles(
        @RequestParam username: String,
        filePart: FilePart,
    ): Mono<EmptyResponse> = Mono.empty()

    @PostMapping("/upload-tests")
    fun uploadTests(
        @RequestParam username: String,
        fileParts: List<FilePart>,
    ): Mono<EmptyResponse> = Mono.empty()
}
