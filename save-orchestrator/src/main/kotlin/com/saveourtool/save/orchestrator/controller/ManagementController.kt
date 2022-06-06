package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/internal/management")
class ManagementController(
    private val dockerService: DockerService,
    private val agentService: AgentService,
) {
    @PostMapping("/docker/images/build-base", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun buildBaseImage(@RequestBody request: String): Mono<ResponseEntity<Void>> =
        Mono.just(request)
            .map { Json.decodeFromString<Sdk>(it) }
            .doOnSuccess { sdk ->
                Mono.fromCallable {
                    dockerService.buildBaseImage(sdk)
                }
                    .subscribeOn(agentService.scheduler)
                    .subscribe()
            }
            .then(Mono.just(ResponseEntity.accepted().build()))
}
