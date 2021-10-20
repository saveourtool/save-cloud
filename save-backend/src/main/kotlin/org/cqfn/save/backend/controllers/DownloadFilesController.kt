package org.cqfn.save.backend.controllers

import org.cqfn.save.backend.ByteArrayResponse
import org.cqfn.save.backend.repository.TimestampBasedFileSystemRepository
import org.cqfn.save.domain.FileInfo
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.io.FileNotFoundException
import kotlin.io.path.fileSize
import kotlin.io.path.name

/**
 * A Spring controller for file downloading
 */
@RestController
class DownloadFilesController(
    private val fileSystemRepository: TimestampBasedFileSystemRepository,
) {
    private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)

    /**
     * @return a list of files in [fileSystemRepository]
     */
    @GetMapping("/files/list")
    fun list(): List<FileInfo> = fileSystemRepository.getFilesList().map {
        FileInfo(
            it.name,
            // assuming here, that we always store files in timestamp-based directories
            it.parent.name.toLong(),
            it.fileSize(),
        )
    }

    /**
     * @param fileInfo a FileInfo based on which a file should be located
     * @return [Mono] with file contents
     */
    @GetMapping(value = ["/files/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(@RequestBody fileInfo: FileInfo): Mono<ByteArrayResponse> = Mono.fromCallable {
        logger.info("Sending file ${fileInfo.name} to a client")
        ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(
            fileSystemRepository.getFile(fileInfo).inputStream.readAllBytes()
        )
    }
        .doOnError(FileNotFoundException::class.java) {
            logger.warn("File $fileInfo is not found", it)
        }
        .onErrorReturn(
            FileNotFoundException::class.java,
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        )

    /**
     * @param file a file to be uploaded
     * @return [Mono] with response
     */
    @PostMapping(value = ["/files/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("file") file: Mono<FilePart>) =
            fileSystemRepository.saveFile(file).map { fileInfo ->
                ResponseEntity.status(
                    if (fileInfo.sizeBytes > 0) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
                )
                    .body(fileInfo)
            }
                .onErrorReturn(
                    FileAlreadyExistsException::class.java,
                    ResponseEntity.status(HttpStatus.CONFLICT).build()
                )
}
