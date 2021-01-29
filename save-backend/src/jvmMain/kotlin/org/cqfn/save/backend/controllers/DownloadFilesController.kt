package org.cqfn.save.backend.controllers

import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File




@Controller
class DownloadFilesController {
    @GetMapping(value = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @ResponseBody
    fun download(): Mono<ByteArray> {
        return Mono.fromCallable {
            val file = File("test.txt")
            file.createNewFile()
            file.bufferedWriter().use {
                it.write("qweqwe")
            }
            file.readBytes()
        }
    }

    @PostMapping(value = ["/upload"],  consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("files") file: Flux<FilePart>): Mono<ServerResponse> {
        return ServerResponse.ok().bodyValue("SOME")
    }
}