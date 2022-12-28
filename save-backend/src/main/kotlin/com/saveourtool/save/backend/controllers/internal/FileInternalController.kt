package com.saveourtool.save.backend.controllers.internal

import com.saveourtool.save.backend.ByteBufferFluxResponse
import com.saveourtool.save.backend.controllers.DownloadFilesController
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.switchIfEmptyToNotFound
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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
     * @param organizationName
     * @param projectName
     * @param name
     * @param uploadedMillis
     * @return [Mono] with file contents
     */
    @PostMapping(path = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun internalDownload(
        @RequestParam organizationName: String,
        @RequestParam projectName: String,
        @RequestParam name: String,
        @RequestParam uploadedMillis: Long,
    ): Mono<ByteBufferFluxResponse> = FileKey(
        projectCoordinates = ProjectCoordinates(
            organizationName = organizationName,
            projectName = projectName,
        ),
        name = name,
        uploadedMillis = uploadedMillis,
    )
        .toMono()
        .flatMap { fileKey ->
            fileStorage.doesExist(fileKey)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "File with key $fileKey is not found"
                }
                .map {
                    log.info("Sending file ${fileKey.name} to an agent")
                    ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(fileStorage.download(fileKey))
                }
        }

    companion object {
        private val log = LoggerFactory.getLogger(DownloadFilesController::class.java)
    }
}
