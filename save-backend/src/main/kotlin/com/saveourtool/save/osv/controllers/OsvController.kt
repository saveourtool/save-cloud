package com.saveourtool.save.osv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.entities.vulnerability.*
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
     * @param name vulnerability name in save db
     * @return content of OSV
     */
    @GetMapping(path = ["/get-by-save-name/{name}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBySaveName(
        @PathVariable name: String,
    ): Mono<StringResponse> = osvService.findBySaveName(name)
        .map {
            ResponseEntity.ok(Json.encodeToString(it))
        }

    /**
     * @param content
     * @param authentication
     * @return list of save's vulnerability names
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/upload")
    fun upload(
        @RequestBody content: String,
        authentication: Authentication,
    ): Mono<StringListResponse> = osvService.decodeAndSave(content, authentication)
        .collectList()
        .map {
            ResponseEntity.ok(it)
        }

    /**
     * @param filePartFlux
     * @param authentication
     * @return list of save's vulnerability names
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping(path = ["/batch-upload"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun batchUpload(
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
                    osvService.decodeAndSave(it, authentication)
                }
        }
        .collectList()
        .map { ResponseEntity.ok(it) }

    companion object {
        @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
        private val log = getLogger<OsvController>()
    }
}
