package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.domain.Python
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux

class BaseImageBuildingJob(
    configProperties: ConfigProperties,
) : QuartzJobBean() {
    private val orchestratorWebClient = WebClient.create(configProperties.orchestratorUrl)

    override fun executeInternal(context: JobExecutionContext) {
        Flux.fromIterable(
            Jdk.versions.map { Jdk(it) } + Python.versions.map { Python(it) }
        ).flatMap { sdk ->
            orchestratorWebClient.post()
                .uri("/internal/management/docker/images/build-base")
                .bodyValue(sdk)
                .retrieve()
                .toBodilessEntity()
        }
            .blockLast()
    }
}