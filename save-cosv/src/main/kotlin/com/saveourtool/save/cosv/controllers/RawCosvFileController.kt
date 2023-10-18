package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entities.cosv.RawCosvFileStreamingResponse
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import org.reactivestreams.Publisher
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.io.path.*

typealias RawCosvFileDtoList = List<RawCosvFileDto>
typealias RawCosvFileDtoFlux = Flux<RawCosvFileDto>
typealias RawCosvFileStreamingResponseFlux = Flux<RawCosvFileStreamingResponse>

/**
 * Rest controller for raw COSV files
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/raw-cosv/{organizationName}")
class RawCosvFileController(
    private val cosvService: CosvService,
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val backendService: IBackendService,
) {
    private fun createTempDirectoryForArchive() = Files.createTempDirectory(
        backendService.workingDir.createDirectories(),
        "archive-"
    )

    /**
     * @param organizationName
     * @param filePartMono
     * @param authentication
     * @param contentLength
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartMono: Mono<FilePart>,
        @RequestHeader(CONTENT_LENGTH_CUSTOM) contentLength: Long,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMapMany {
            filePartMono
                .flatMapMany { filePart ->
                    doUpload(filePart, organizationName, authentication.name, contentLength)
                }
        }

    /**
     * @param organizationName
     * @param filePartFlux
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(
        "/batch-upload",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun batchUpload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartFlux: Flux<FilePart>,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileDtoFlux> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMapMany {
            filePartFlux
                .flatMap { filePart ->
                    doUpload(filePart, organizationName, authentication.name, contentLength = null)
                }
        }
        .let {
            ResponseEntity
                .ok()
                .cacheControlForNdjson()
                .body(it)
        }

    private fun doUpload(
        filePart: FilePart,
        organizationName: String,
        userName: String,
        contentLength: Long?,
    ): Publisher<RawCosvFileDto> {
        log.debug {
            "Processing ${filePart.filename()}"
        }
        val key = RawCosvFileDto(
            filePart.filename(),
            organizationName = organizationName,
            userName = userName,
            contentLength = contentLength,
        )
        val content = filePart.content().map { it.asByteBuffer() }
        return rawCosvFileStorage.uploadAndWrapDuplicateKeyException(key, content)
    }

    /**
     * @param organizationName
     * @param id
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(
        "/unzip/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun unzip(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileStreamingResponseFlux> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMap { rawCosvFileStorage.findById(id) }
        .flatMapMany { rawCosvFile ->
            Flux.concat(
                Mono.just(firstFakeResponse),
                doUploadArchiveEntries(
                    rawCosvFile,
                    organizationName,
                    authentication.name
                )
            )
        }
        .let {
            ResponseEntity.ok()
                .cacheControlForNdjson()
                .body(it)
        }

    private fun doUploadArchiveEntries(
        archiveFile: RawCosvFileDto,
        organizationName: String,
        userName: String,
    ): RawCosvFileStreamingResponseFlux = blockingToMono {
        val tmpDir = createTempDirectoryForArchive()
        val tmpArchiveFile = tmpDir / archiveFile.fileName
        val contentDir = Files.createTempDirectory(tmpDir, "content-")
        Triple(tmpDir, tmpArchiveFile, contentDir)
    }
        .flatMapMany { (tmpDir, tmpArchiveFile, contentDir) ->
            log.debug {
                "Saving archive ${archiveFile.fileName} to ${tmpArchiveFile.absolutePathString()}"
            }
            rawCosvFileStorage.download(archiveFile)
                .collectToFile(tmpArchiveFile)
                .blockingMap {
                    tmpArchiveFile.extractZipTo(contentDir)
                    val entries = contentDir.listDirectoryEntries()
                    entries.map { it to it.fileSize() } to tmpArchiveFile.fileSize()
                }
                .flatMapMany { (entryWithSizeList, archiveSize) ->
                    val fullSize = archiveSize * 2 + entryWithSizeList.sumOf { it.second }
                    Flux.concat(
                        Mono.just(RawCosvFileStreamingResponse(archiveSize, fullSize, updateCounters = true)),
                        Flux.fromIterable(entryWithSizeList.map { it.first })
                            .flatMap { file ->
                                log.debug {
                                    "Processing ${file.absolutePathString()}"
                                }
                                val contentLength = file.fileSize()
                                rawCosvFileStorage.uploadAndWrapDuplicateKeyException(
                                    key = RawCosvFileDto(
                                        concatS3Key(archiveFile.fileName, file.relativeTo(contentDir).toString()),
                                        organizationName = organizationName,
                                        userName = userName,
                                        contentLength = contentLength,
                                    ),
                                    content = file.toByteBufferFlux(),
                                )
                                    .map { RawCosvFileStreamingResponse(contentLength, fullSize, result = listOf(it)) }
                            },
                        blockingToMono {
                            tmpDir.deleteRecursivelySafely(log)
                        }
                            .then(rawCosvFileStorage.delete(archiveFile))
                            .thenReturn(RawCosvFileStreamingResponse(archiveSize, fullSize, updateCounters = false)),
                    )
                }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to process archive ${archiveFile.fileName}"
                    }
                    blockingToMono { tmpDir.deleteRecursivelySafely(log) }.then(Mono.error(error))
                }
        }

    /**
     * @param organizationName
     * @param ids
     * @param authentication
     * @return [StringResponse]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/submit-to-process")
    fun submitToProcess(
        @PathVariable organizationName: String,
        @RequestBody ids: List<Long>,
        authentication: Authentication,
    ): Mono<StringResponse> = doSubmitToProcess(organizationName, ids, authentication)

    /**
     * @param organizationName
     * @param authentication
     * @return [StringResponse]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/submit-all-uploaded-to-process")
    fun submitAllUploadedToProcess(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = rawCosvFileStorage.listByOrganizationAndUser(organizationName, authentication.name)
        .map { files ->
            files.filter { it.userName == authentication.name }.map { it.requiredId() }
        }
        .flatMap { ids ->
            doSubmitToProcess(organizationName, ids, authentication)
        }

    private fun doSubmitToProcess(
        organizationName: String,
        ids: List<Long>,
        authentication: Authentication,
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.WRITE, "submit to process")
        .flatMap {
            rawCosvFileStorage.updateAll(ids, RawCosvFileStatus.IN_PROGRESS)
        }
        .thenReturn(ResponseEntity.ok("Submitted $ids to be processed"))
        .doOnSuccess {
            blockingToMono {
                backendService.getOrganizationByName(organizationName) to backendService.getUserByName(authentication.name)
            }
                .flatMap { (organization, user) ->
                    cosvService.process(ids, user, organization)
                }
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }

    /**
     * @param organizationName
     * @param authentication
     * @return count of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/count")
    fun count(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<Long> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMap {
            rawCosvFileStorage.countByOrganizationAndUser(organizationName, authentication.name)
        }

    /**
     * @param organizationName
     * @param authentication
     * @param page
     * @param size
     * @return list of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping(
        "/list",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun list(
        @PathVariable organizationName: String,
        @RequestParam page: Int,
        @RequestParam size: Int,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileDtoFlux> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMap {
            rawCosvFileStorage.listByOrganizationAndUser(organizationName, authentication.name, PageRequest.of(page, size))
        }
        .flatMapIterable { it }
        .let {
            ResponseEntity.ok()
                .cacheControlForNdjson()
                .body(it)
        }

    /**
     * @param organizationName
     * @param id ID of raw COSV file
     * @param authentication
     * @return content of Raw COSV file
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/download/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = hasPermission(authentication, organizationName, Permission.READ, "download")
        .flatMap {
            rawCosvFileStorage.findById(id)
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${it.fileName}\"")
                .body(rawCosvFileStorage.download(it))
        }

    /**
     * @param organizationName
     * @param id
     * @param authentication
     * @return string response
     */
    @RequiresAuthorizationSourceHeader
    @DeleteMapping("/delete/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.DELETE, "delete")
        .flatMap {
            rawCosvFileStorage.deleteById(id)
                .filter { it }
                .switchIfEmptyToNotFound {
                    "Raw COSV file not found by id $id"
                }
                .map {
                    ResponseEntity.ok("Raw COSV file deleted successfully")
                }
        }

    /**
     * @param organizationName
     * @param authentication
     * @return list of deleted keys
     */
    @RequiresAuthorizationSourceHeader
    @DeleteMapping(
        "/delete-processed",
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun deleteProcessed(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): ResponseEntity<RawCosvFileStreamingResponseFlux> = hasPermission(authentication, organizationName, Permission.DELETE, "delete")
        .flatMapMany {
            Flux.concat(
                firstFakeResponse.toMono(),
                doDeleteProcessed(organizationName, authentication.name),
            )
        }
        .let {
            ResponseEntity.ok()
                .cacheControlForNdjson()
                .body(it)
        }

    private fun doDeleteProcessed(
        organizationName: String,
        userName: String,
    ) = rawCosvFileStorage.listByOrganizationAndUser(organizationName, userName)
        .map { files ->
            files.filter { it.status == RawCosvFileStatus.PROCESSED }
                .run {
                    sumOf { it.requiredContentLength() } to windowed(WINDOW_SIZE_ON_DELETE)
                }
        }
        .flatMapMany { (sizeOfFiles, parts) ->
            val fullSize = sizeOfFiles + 1
            Flux.concat(
                RawCosvFileStreamingResponse(
                    1,
                    fullSize,
                    updateCounters = true,
                ).toMono(),
                parts.toFlux().flatMap { keys ->
                    rawCosvFileStorage.deleteAll(keys)
                        .filter { it }
                        .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
                            "Failed to delete process raw cosv files: $keys"
                        }
                        .thenReturn(
                            RawCosvFileStreamingResponse(
                                keys.sumOf { it.requiredContentLength() },
                                fullSize,
                                result = keys,
                            )
                        )
                },
            )
        }

    private fun hasPermission(
        authentication: Authentication,
        organizationName: String,
        permission: Permission,
        operation: String,
    ): Mono<Unit> = blockingToMono {
        backendService.hasPermissionInOrganization(authentication, organizationName, permission)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to $operation raw COSV files from $organizationName"
        }
        .let { validated ->
            validated.takeIf { permission == Permission.WRITE }
                ?.validateCanDoBulkUpload(authentication, organizationName)
                ?: validated
        }
        .thenReturn(Unit)

    private fun Mono<*>.validateCanDoBulkUpload(
        authentication: Authentication,
        organizationName: String,
    ) = blockingMap {
        backendService.getUserPermissionsByOrganizationName(authentication, organizationName)
    }
        .filter { it.inOrganizations[organizationName]?.canDoBulkUpload == true }
        .orResponseStatusException(HttpStatus.FORBIDDEN) {
            "You do not have permission to upload COSV files on behalf of this organization: $organizationName"
        }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvController>()
        private const val WINDOW_SIZE_ON_DELETE = 10

        // to show progress bar
        private val firstFakeResponse = RawCosvFileStreamingResponse(5, 100, updateCounters = true)

        private fun RawCosvFileStorage.uploadAndWrapDuplicateKeyException(
            key: RawCosvFileDto,
            content: Flux<ByteBuffer>,
        ): Mono<RawCosvFileDto> {
            val result = key.contentLength?.let {
                upload(key, it, content)
            } ?: upload(key, content)
            return result.onErrorResume { error ->
                when (error) {
                    is DataIntegrityViolationException -> Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate file name ${key.fileName}", error))
                    else -> Mono.error(error)
                }
            }
        }
    }
}
