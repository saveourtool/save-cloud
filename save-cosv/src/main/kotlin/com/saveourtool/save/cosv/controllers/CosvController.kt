package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.cosv.service.CosvService
import com.saveourtool.save.entities.cosv.CosvFileDto
import com.saveourtool.save.v1

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

import java.nio.ByteBuffer

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
