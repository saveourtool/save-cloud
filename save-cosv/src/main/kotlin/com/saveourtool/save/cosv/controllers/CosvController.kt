package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.configs.RequiresAuthorizationSourceHeader
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.cosv.service.OrganizationService
import com.saveourtool.save.cosv.service.UserService
import com.saveourtool.save.entities.cosv.CosvFileDto
import com.saveourtool.save.entities.cosv.VulnerabilityMetadataDto
import com.saveourtool.save.utils.*
import com.saveourtool.save.v1
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

import java.nio.ByteBuffer

/**
 * Rest controller for COSVs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/cosv")
class CosvController(
    private val cosvService: CosvService,
    private val userService: UserService,
    private val organizationService: OrganizationService,
) {
    /**
     * @param cosv
     * @param isGenerateIdentifier
     * @param organizationName
     * @param authentication
     * @return saved [VulnerabilityMetadataDto]
     */
    @RequiresAuthorizationSourceHeader
    @PostMapping("/save")
    fun save(
        @RequestBody cosv: ManualCosvSchema,
        @RequestParam(required = false, defaultValue = "false") isGenerateIdentifier: Boolean,
        @RequestParam(required = false) organizationName: String?,
        authentication: Authentication,
    ): Mono<VulnerabilityMetadataDto> = cosv.id.toMono()
        .filter { identifier -> isGenerateIdentifier && identifier.isEmpty() || identifier.isNotEmpty() }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "Identifier is not provided: either set identifier auto-generation and provide no identifier or provide an identifier."
        }
        .filter { validateIdentifier(it) }
        .switchIfEmptyToResponseException(HttpStatus.CONFLICT) {
            "COSV identifier should either be empty or start with one of prefixes: $vulnerabilityPrefixes"
        }
        .blockingMap {
            val user = userService.getUserByName(authentication.name)
            val organization = organizationName?.let {
                organizationService.getOrganizationByName(it)
            }
            val generatedId = if (isGenerateIdentifier) cosvService.generateIdentifier() else null
            Triple(user, organization, generatedId)
        }
        .flatMap { (user, organization, generatedId) ->
            cosvService.saveManual(
                generatedId?.let { cosv.copy(id = it) } ?: cosv,
                user,
                organization,
            )
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
}
