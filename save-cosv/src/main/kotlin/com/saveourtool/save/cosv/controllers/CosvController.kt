package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.cosv.CosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.entities.cosv.UnzipRawCosvFileResponse
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1

import org.reactivestreams.Publisher
import org.springframework.data.domain.PageRequest
import org.springframework.http.*
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

import java.nio.ByteBuffer
import java.nio.file.Files

import kotlin.io.path.*

typealias RawCosvFileDtoList = List<RawCosvFileDto>
typealias RawCosvFileDtoFlux = Flux<RawCosvFileDto>
typealias UnzipRawCosvFileResponseFlux = Flux<UnzipRawCosvFileResponse>

/**
 * Rest controller for COSVs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/cosv")
class CosvController(
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
    @PostMapping("/{organizationName}/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
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
        "/{organizationName}/batch-upload",
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
        )
        val content = filePart.content().map { it.asByteBuffer() }
        return contentLength?.let {
            rawCosvFileStorage.upload(key, it, content)
        } ?: rawCosvFileStorage.upload(key, content)
    }

    /**
     * @param organizationName
     * @param id
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(
        "/{organizationName}/unzip/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_NDJSON_VALUE],
    )
    fun unzip(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): ResponseEntity<UnzipRawCosvFileResponseFlux> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
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
    ): UnzipRawCosvFileResponseFlux = blockingToMono {
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
                        Mono.just(UnzipRawCosvFileResponse(archiveSize, fullSize, updateCounters = true)),
                        Flux.fromIterable(entryWithSizeList.map { it.first })
                            .flatMap { file ->
                                log.debug {
                                    "Processing ${file.absolutePathString()}"
                                }
                                val contentLength = file.fileSize()
                                rawCosvFileStorage.upload(
                                    key = RawCosvFileDto(
                                        concatS3Key(archiveFile.fileName, file.relativeTo(contentDir).toString()),
                                        organizationName = organizationName,
                                        userName = userName,
                                    ),
                                    contentLength = contentLength,
                                    content = file.toByteBufferFlux(),
                                )
                                    .map { UnzipRawCosvFileResponse(contentLength, fullSize, result = it) }
                            },
                        blockingToMono {
                            tmpDir.deleteRecursivelySafely(log)
                        }
                            .then(rawCosvFileStorage.delete(archiveFile))
                            .thenReturn(UnzipRawCosvFileResponse(archiveSize, fullSize, updateCounters = false)),
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
    @PostMapping("/{organizationName}/submit-to-process")
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
    @PostMapping("/{organizationName}/submit-all-uploaded-to-process")
    fun submitAllUploadedToProcess(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<StringResponse> = rawCosvFileStorage.listByOrganization(organizationName)
        .filter {
            it.userName == authentication.name
        }
        .map { it.requiredId() }
        .collectList()
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
    @GetMapping("/{organizationName}/count")
    fun count(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<Long> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMap {
            rawCosvFileStorage.countByOrganization(organizationName)
        }

    /**
     * @param organizationName
     * @param authentication
     * @param page
     * @param size
     * @return list of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/{organizationName}/list")
    fun list(
        @PathVariable organizationName: String,
        @RequestParam page: Int,
        @RequestParam size: Int,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMapMany {
            rawCosvFileStorage.listByOrganization(organizationName, PageRequest.of(page, size))
        }

    /**
     * @param identifier
     * @return list of cosv files
     */
    @GetMapping("/list-versions")
    fun listVersions(
        @RequestParam identifier: String,
    ): Flux<CosvFileDto> = cosvService.listVersions(identifier)

    /**
     * @param cosvFileId
     * @return cosv file content
     */
    @GetMapping("/cosv-content")
    fun cosvFileContent(
        @RequestParam cosvFileId: Long,
    ): Flux<ByteBuffer> = cosvService.getVulnerabilityVersionAsCosvStream(cosvFileId)

    /**
     * @param organizationName
     * @param id ID of raw COSV file
     * @param authentication
     * @return content of Raw COSV file
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/{organizationName}/download/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
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
    @DeleteMapping("/{organizationName}/delete/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
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
    @DeleteMapping("/{organizationName}/delete-processed", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteProcessed(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Mono<RawCosvFileDtoList> = hasPermission(authentication, organizationName, Permission.DELETE, "delete")
        .flatMap {
            rawCosvFileStorage.listByOrganization(organizationName)
                .filter { it.status == RawCosvFileStatus.PROCESSED }
                .collectList()
                .flatMap { keys ->
                    rawCosvFileStorage.deleteAll(keys)
                        .filter { it }
                        .switchIfEmptyToResponseException(HttpStatus.INTERNAL_SERVER_ERROR) {
                            "Failed to delete process raw cosv files: $keys"
                        }
                        .map { keys }
                }
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

        // to show progress bar
        private val firstFakeResponse = UnzipRawCosvFileResponse(5, 100, updateCounters = true)
    }
}
