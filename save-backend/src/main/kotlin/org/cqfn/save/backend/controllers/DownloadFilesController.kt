package org.cqfn.save.backend.controllers

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.io.File

/**
 * A Spring controller for file downloading
 */
@RestController
class DownloadFilesController {
    private val logger = LoggerFactory.getLogger("org.cqfn.save.logback")

    /**
     * @return [Mono] with file contents
     */
    @GetMapping(value = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download() = Mono.fromCallable {
        val file = File("test.txt")
        file.createNewFile()
        file.bufferedWriter().use {
            it.write("qweqwe")
        }
        file.readBytes()
    }

    /**
     * @param file a file to be uploaded
     * @return [Mono] with response
     */
    @PostMapping(value = ["/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("file") file: Mono<FilePart>) = Mono.just(ResponseEntity.ok().body("test"))
}
