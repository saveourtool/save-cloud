package com.saveourtool.save.cosv.controllers

import com.saveourtool.save.configs.ApiSwaggerSupport
import com.saveourtool.save.cosv.cvsscalculator.calculateScore
import com.saveourtool.save.utils.StringResponse
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.v1

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Rest controller for CVSSs
 */
@ApiSwaggerSupport
@RestController
@RequestMapping("/api/$v1/cvss")
class CvssController {
    /**
     * @param vector
     * @return base score criticality
     */
    @GetMapping("/get-base-score")
    fun getBaseScore(
        @RequestParam vector: String,
    ): Mono<StringResponse> = blockingToMono {
        calculateScore(vector)
    }.map {
        ResponseEntity.ok(Json.encodeToString(it))
    }
}
