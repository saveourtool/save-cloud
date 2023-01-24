package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Internal controller for [FileStorage]
 *
 * @property fileStorage
 */
@RestController
@RequestMapping("/internal/files")
class FileInternalController(
    private val fileStorage: FileStorage,
) {
    /**
     * @param fileId
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun internalDownload(
        @RequestParam fileId: Long,
    ): Mono<ByteBufferFluxResponse> = fileStorage.getFileById(fileId)
        .flatMap { fileDto ->
            fileStorage.doesExist(fileDto)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "File with key $fileDto is not found"
                }
                .map {
                    log.info("Sending file ${fileDto.name} to an agent")
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileStorage.download(fileDto))
                }
        }

    companion object {
        private val log = LoggerFactory.getLogger(FileInternalController::class.java)
    }
}
