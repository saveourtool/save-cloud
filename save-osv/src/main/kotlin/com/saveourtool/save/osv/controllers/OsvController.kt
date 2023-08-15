package com.saveourtool.save.osv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.entities.vulnerability.*
import com.saveourtool.save.osv.processor.DefaultOsvProcessor
import com.saveourtool.save.osv.service.OsvService
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
 * Rest controller for OSVs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/osv")
class OsvController(
    private val osvService: OsvService,
) {
    /**
     * @param id vulnerability name in save db
     * @return content of OSV
     */
    @RequiresAuthorizationSourceHeader
    @GetMapping(path = ["/get-by-id/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getById(
        @PathVariable id: String,
    ): Mono<StringResponse> = osvService.findById(id)
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
        @RequestParam(required = false, defaultValue = DefaultOsvProcessor.ID) sourceId: String,
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = osvService.decodeAndSave(sourceId, content, authentication)
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
        @RequestParam(required = false, defaultValue = DefaultOsvProcessor.ID) sourceId: String,
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
                .flatMapMany {
                    osvService.decodeAndSave(sourceId, it, authentication)
                }
        }
        .collectList()
        .map { ResponseEntity.ok(it) }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<OsvController>()
    }
}
