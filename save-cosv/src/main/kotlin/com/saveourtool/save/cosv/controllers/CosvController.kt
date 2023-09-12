package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.entities.cosv.RawCosvFileDto
import com.saveourtool.save.entities.cosv.RawCosvFileStatus
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
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

/**
 * Rest controller for COSVs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/cosv/{organizationName}")
class CosvController(
    private val cosvService: CosvService,
    private val rawCosvFileStorage: RawCosvFileStorage,
    private val backendService: IBackendService,
) {
    /**
     * @param organizationName
     * @param filePartFlux
     * @param authentication
     * @return list of uploaded [RawCosvFileDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/batch-upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun batchUpload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartFlux: Flux<FilePart>,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.WRITE, "upload")
        .flatMapMany {
            filePartFlux
                .flatMap { filePart ->
                    log.debug {
                        "Processing ${filePart.filename()}"
                    }
                    rawCosvFileStorage.upload(
                        key = RawCosvFileDto(
                            filePart.filename(),
                            organizationName = organizationName,
                            userName = authentication.name,
                        ),
                        content = filePart.content().map { it.asByteBuffer() },
                    )
                }
                .parallel()
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
    ): Mono<StringResponse> = hasPermission(authentication, organizationName, Permission.WRITE, "submit to process")
        .flatMap {
            rawCosvFileStorage.updateAll(ids, RawCosvFileStatus.IN_PROGRESS)
        }
        .thenReturn(ResponseEntity.ok("Submitted $ids to be processed"))
        .doOnSuccess {
            cosvService.process(ids)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe()
        }

    /**
     * @param organizationName
     * @param authentication
     * @return list of uploaded raw cosv files in [organizationName]
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/list")
    fun list(
        @PathVariable organizationName: String,
        authentication: Authentication,
    ): Flux<RawCosvFileDto> = hasPermission(authentication, organizationName, Permission.READ, "read")
        .flatMapMany {
            rawCosvFileStorage.listByOrganization(organizationName)
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
     * @return empty response
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
