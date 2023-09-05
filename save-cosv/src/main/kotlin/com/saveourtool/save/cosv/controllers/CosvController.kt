package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.entities.cosv.RawCosvExt
import com.saveourtool.save.entities.vulnerability.VulnerabilityStatus
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Rest controller for COSVs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/cosv")
class CosvController(
    private val cosvService: CosvService,
) {
    @GetMapping("/by-cosv-id-and-status")
    @Operation(
        method = "GET",
        summary = "Get COSV by name.",
        description = "Get COSV by name.",
    )
    @ApiResponse(responseCode = "200", description = "Successfully fetched vulnerability by name")
    fun getByCosvIdAndActive(
        @RequestParam cosvId: String,
        @RequestParam status: VulnerabilityStatus,
    ): Mono<RawCosvExt> = cosvService.getByCosvIdAndStatus(cosvId, status).switchIfEmptyToNotFound()

    /**
     * @param cosvId COSV identifier
     * @return content of COSV
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping(path = ["/get-by-id/{cosvId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getById(
        @PathVariable cosvId: String,
    ): Mono<StringResponse> = cosvService.findById(cosvId)
        .map {
            ResponseEntity.ok(Json.encodeToString(it))
        }

    /**
     * @param content
     * @param authentication
     * @param organizationName
     * @return list of save's vulnerability identifiers
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload")
    fun upload(
        @RequestParam organizationName: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = cosvService.decodeAndSave(content, authentication, organizationName)
        .collectList()
        .map {
            ResponseEntity.ok(it)
        }

    /**
     * @param filePartFlux
     * @param authentication
     * @param organizationName
     * @return list of save's vulnerability identifiers
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(path = ["/batch-upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun batchUpload(
        @RequestParam organizationName: String,
        @RequestPart(FILE_PART_NAME) filePartFlux: Flux<FilePart>,
        authentication: Authentication,
    ): Mono<StringListResponse> = filePartFlux
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

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvController>()
    }
}
