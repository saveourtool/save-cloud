package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.backend.service.IBackendService
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.storage.RawCosvFileStorage
import com.saveourtool.save.permission.Permission
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

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
     * @param content
     * @param authentication
     * @return list of save's vulnerability identifiers
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload")
    fun upload(
        @PathVariable organizationName: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = blockingToMono {
        backendService.hasPermissionInOrganization(authentication, organizationName, Permission.WRITE)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to upload raw COSV files from $organizationName"
        }
        .flatMap {
            cosvService.decodeAndSave(content, authentication, organizationName)
                .collectList()
                .map {
                    ResponseEntity.ok(it)
                }
        }

    /**
     * @param organizationName
     * @param filePartFlux
     * @param authentication
     * @return list of save's vulnerability identifiers
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(path = ["/batch-upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun batchUpload(
        @PathVariable organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartFlux: Flux<FilePart>,
        authentication: Authentication,
    ): Mono<StringListResponse> = blockingToMono {
        backendService.hasPermissionInOrganization(authentication, organizationName, Permission.WRITE)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to upload raw COSV files from $organizationName"
        }
        .flatMap {
            filePartFlux
                .flatMap { filePart ->
                    log.debug {
                        "Processing ${filePart.filename()}"
                    }
                    filePart.content()
                        .map { it.asByteBuffer() }
                        .collectToInputStream()
                }
                .let { inputStreams ->
                    cosvService.decodeAndSave(inputStreams, authentication, organizationName)
                }
                .collectList()
                .map { ResponseEntity.ok(it) }
        }

    /**
     * @param id ID of raw COSV file
     * @return content of Raw COSV file
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping("/download/{id}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun download(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<ByteBufferFluxResponse> = blockingToMono {
        backendService.hasPermissionInOrganization(authentication, organizationName, Permission.READ)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to download raw COSV files from $organizationName"
        }
        .map {
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(rawCosvFileStorage.downloadById(id))
        }

    /**
     * @param id
     * @return empty response
     */
    @RequiresAuthorizationSourceHeader
    @DeleteMapping("/delete/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun delete(
        @PathVariable organizationName: String,
        @PathVariable id: Long,
        authentication: Authentication,
    ): Mono<StringResponse> = blockingToMono {
        backendService.hasPermissionInOrganization(authentication, organizationName, Permission.DELETE)
    }
        .filter { it }
        .switchIfEmptyToResponseException(HttpStatus.FORBIDDEN) {
            "${authentication.name} has no permission to delete raw COSV files from $organizationName"
        }
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

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvController>()
    }
}
