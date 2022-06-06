package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.domain.Python
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.slf4j.LoggerFactory
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class BaseImageBuildingJob(
    configProperties: ConfigProperties,
) : QuartzJobBean() {
    private val orchestratorWebClient = WebClient.create(configProperties.orchestratorUrl)

    override fun executeInternal(context: JobExecutionContext) {
        Flux.fromIterable(
            Jdk.versions.map { Jdk(it) } + Python.versions.map { Python(it) }
        ).flatMap { sdk ->
            logger.info("Requesting base image build for sdk=$sdk")
            orchestratorWebClient.post()
                .uri("/internal/management/docker/images/build-base")
                .bodyValue(sdk)
                .retrieve()
                .toBodilessEntity()
                .onErrorResume {
                    logger.warn("Couldn't request base image build for sdk=$sdk", it)
                    Mono.empty()
                }
        }
            .blockLast()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BaseImageBuildingJob::class.java)
        val jobKey = JobKey(BaseImageBuildingJob::class.simpleName)
    }
}
