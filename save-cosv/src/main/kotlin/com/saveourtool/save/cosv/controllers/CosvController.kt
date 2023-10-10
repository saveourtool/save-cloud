package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.cosv.CosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.storage.concatS3Key
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import org.reactivestreams.Publisher
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.io.path.*

typealias RawCosvFileDtoFlux = Flux<RawCosvFileDto>
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
    ): ResponseEntity<RawCosvFileDtoFlux> = withHttpHeaders {
        hasPermission(authentication, organizationName, Permission.WRITE, "upload")
            .flatMapMany {
                filePartFlux
                    .flatMap { filePart ->
                        doUpload(filePart, organizationName, authentication.name, contentLength = null)
                    }
            }
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
        return if (!filePart.filename().endsWith(ARCHIVE_EXTENSION, ignoreCase = true)) {
            val key = RawCosvFileDto(
                filePart.filename(),
                organizationName = organizationName,
                userName = userName,
            )
            val content = filePart.content().map { it.asByteBuffer() }
            contentLength?.let {
                rawCosvFileStorage.upload(key, it, content)
            } ?: rawCosvFileStorage.upload(key, content)
        } else {
            doArchiveUpload(filePart, organizationName, userName)
        }
    }

    private fun doArchiveUpload(
        archiveFilePart: FilePart,
        organizationName: String,
        userName: String,
    ): Flux<RawCosvFileDto> = blockingToMono {
        val tmpDir = createTempDirectoryForArchive()
        tmpDir to Files.createTempDirectory(tmpDir, "content-")
    }
        .flatMapMany { (tmpDir, contentDir) ->
            val archiveFile = tmpDir / archiveFilePart.filename()
            log.debug {
                "Saving archive ${archiveFilePart.filename()} to ${archiveFile.absolutePathString()}"
            }
            archiveFilePart.transferTo(archiveFile)
                .blockingMap {
                    archiveFile.extractZipTo(contentDir)
                }
                .flatMapMany {
                    blockingToFlux {
                        contentDir.listDirectoryEntries()
                    }
                        .flatMap { file ->
                            log.debug {
                                "Processing ${file.absolutePathString()}"
                            }
                            rawCosvFileStorage.upload(
                                key = RawCosvFileDto(
                                    concatS3Key(archiveFilePart.filename(), file.relativeTo(contentDir).toString()),
                                    organizationName = organizationName,
                                    userName = userName,
                                ),
                                contentLength = file.fileSize(),
                                content = file.toByteBufferFlux(),
                            )
                        }
                }
                .doOnComplete {
                    tmpDir.deleteRecursivelySafely(log)
                }
                .onErrorResume { error ->
                    log.error(error) {
                        "Failed to process archive ${archiveFilePart.filename()}"
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
     * @return list of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/{organizationName}/list")
    fun list(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMapMany {
            rawCosvFileStorage.listByOrganization(organizationName)
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
     * @return empty response
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
    }
}
