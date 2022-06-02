package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
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
    @PostMapping("/docker/images/build-base")
    fun buildBaseImage(@RequestBody sdk: Sdk) = Mono.just(ResponseEntity.accepted().build<Void>())
        .doOnSuccess {
            dockerService.buildBaseImage(sdk)
        }
        .subscribeOn(agentService.scheduler)
}
