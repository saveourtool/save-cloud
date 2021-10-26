package org.cqfn.save.backend.controllers

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.ByteArrayResponse
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.TestDataFilesystemRepository
import org.cqfn.save.backend.repository.TimestampBasedFileSystemRepository
import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.from

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import java.io.FileNotFoundException

import kotlin.io.path.fileSize
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.readText

/**
 * A Spring controller for file downloading
 */
@RestController
@RequestMapping("/files")
class DownloadFilesController(
    private val additionalToolsFileSystemRepository: TimestampBasedFileSystemRepository,
    private val testDataFilesystemRepository: TestDataFilesystemRepository,
    private val agentRepository: AgentRepository,
) {
    private val logger = LoggerFactory.getLogger(DownloadFilesController::class.java)

    /**
     * @return a list of files in [additionalToolsFileSystemRepository]
     */
    @GetMapping("/list")
    fun list(): List<FileInfo> = additionalToolsFileSystemRepository.getFilesList().map {
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
    @GetMapping(value = ["/download"], produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(@RequestBody fileInfo: FileInfo): Mono<ByteArrayResponse> = Mono.fromCallable {
        logger.info("Sending file ${fileInfo.name} to a client")
        ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(
            additionalToolsFileSystemRepository.getFile(fileInfo).inputStream.readAllBytes()
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
    @PostMapping(value = ["/upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("file") file: Mono<FilePart>) =
            additionalToolsFileSystemRepository.saveFile(file).map { fileInfo ->
                ResponseEntity.status(
                    if (fileInfo.sizeBytes > 0) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR
                )
                    .body(fileInfo)
            }
                .onErrorReturn(
                    FileAlreadyExistsException::class.java,
                    ResponseEntity.status(HttpStatus.CONFLICT).build()
                )

    /**
     * @param testExecutionDto
     * @return [Mono] with response
     * @throws ResponseStatusException
     */
    @PostMapping(value = ["/get-debug-info"])
    fun getDebugInfo(
        @RequestBody testExecutionDto: TestExecutionDto,
    ): String {
        val executionId = agentRepository.findByContainerId(testExecutionDto.agentContainerId!!)!!.execution.id!!
        val testResultLocation = TestResultLocation.from(testExecutionDto)
        val debugInfoFile = testDataFilesystemRepository.getLocation(
            executionId,
            testResultLocation
        )
        return if (debugInfoFile.notExists()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        } else {
            debugInfoFile.readText()
        }
    }

    /**
     * @param agentContainerId agent that has executed the test
     * @param testResultDebugInfo additional info that should be stored
     * @return [Mono] with response
     */
    @PostMapping(value = ["/debug-info"])
    @Suppress("UnsafeCallOnNullableType")
    fun uploadDebugInfo(@RequestParam("agentId") agentContainerId: String,
                        @RequestBody testResultDebugInfo: TestResultDebugInfo,
    ) {
        val executionId = agentRepository.findByContainerId(agentContainerId)!!.execution.id!!
        testDataFilesystemRepository.save(executionId, testResultDebugInfo)
    }
}
