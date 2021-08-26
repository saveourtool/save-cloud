package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.repository.FileSystemRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.io.FileNotFoundException
import java.nio.file.Path

/**
 * A Spring controller for file downloading
 */
@RestController
class DownloadFilesController(
    private val fileSystemRepository: FileSystemRepository,
) {
    private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)

    @GetMapping("/files/list")
    fun list(): List<Path> = fileSystemRepository.getFilesList()

    /**
     * @return [Mono] with file contents
     */
    @GetMapping(value = ["/files/download/{name}"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(@PathVariable("name") name: String): Mono<ResponseEntity<ByteArray>> = Mono.fromCallable {
        logger.info("Sending file $name to a client")
        ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(
            fileSystemRepository.getFile(name).inputStream.readAllBytes()
        )
    }
        .onErrorResume {
            if (it is FileNotFoundException) {
                logger.warn("File $name is not found", it)
                Mono.just(
                    ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .build()
                )
            } else {
                throw it
            }
        }

    /**
     * @param file a file to be uploaded
     * @return [Mono] with response
     */
    @PostMapping(value = ["/files/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("file") file: Mono<FilePart>) =
            fileSystemRepository.saveFile(file).map { size ->
                ResponseEntity.status(
                    if (size > 0) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
                )
                    .body("Saved $size bytes")
            }
}
