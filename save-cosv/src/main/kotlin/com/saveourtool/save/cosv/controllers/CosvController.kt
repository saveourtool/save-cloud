package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.authservice.utils.userId
import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.processor.DefaultCosvProcessor
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1

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
    /**
     * @param id vulnerability name in save db
     * @return content of COSV
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping(path = ["/get-by-id/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getById(
        @PathVariable id: String,
    ): Mono<StringResponse> = cosvService.findById(id)
        .map {
            ResponseEntity.ok(Json.encodeToString(it))
        }

    /**
     * @param sourceId
     * @param content
     * @param authentication
     * @return list of save's vulnerability names
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload")
    fun upload(
        @RequestParam(required = false, defaultValue = DefaultCosvProcessor.ID) sourceId: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = cosvService.decodeAndSave(sourceId, content, authentication)
        .collectList()
        .map {
            ResponseEntity.ok(it)
        }

    /**
     * @param sourceId
     * @param filePartFlux
     * @param authentication
     * @return list of save's vulnerability names
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(path = ["/batch-upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun batchUpload(
        @RequestParam(required = false, defaultValue = DefaultCosvProcessor.ID) sourceId: String,
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
            cosvService.decodeAndSave(sourceId, inputStreams, authentication)
        }
        .collectList()
        .map { ResponseEntity.ok(it) }

    /**
     * @param request
     * @param authentication
     * @return saved save's vulnerability name
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/propose-new")
    fun proposeNew(
        @RequestBody request: ProposeSaveOsvRequest,
        authentication: Authentication,
    ): Mono<StringResponse> = cosvService.createNew(request, authentication.userId())
        .map { ResponseEntity.ok(it) }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<CosvController>()
    }
}
