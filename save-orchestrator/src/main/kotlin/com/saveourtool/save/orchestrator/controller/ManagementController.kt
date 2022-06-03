package com.saveourtool.save.orchestrator.controller

import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.orchestrator.service.AgentService
import com.saveourtool.save.orchestrator.service.DockerService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.RouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.accepted
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

@Configuration
class ManagementController(
    private val dockerService: DockerService,
    private val agentService: AgentService,
) {
    @Bean
    fun managementController() = router {
        "/internal/management".nest {
            "/docker".nest {
                POST("/images/build-base", ::buildBaseImage)
            }
        }
    }

    private fun buildBaseImage(request: ServerRequest): Mono<ServerResponse> =
        request.bodyToMono<Sdk>()
            .doOnSuccess { sdk ->
                Mono.fromCallable {
                    dockerService.buildBaseImage(sdk)
                }
                    .subscribeOn(agentService.scheduler)
                    .subscribe()
            }
            .then(accepted().build())
}
