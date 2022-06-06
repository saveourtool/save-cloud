package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * a [Job] that commands preprocessor to update standard test suites
 */
class UpdateJob(
    configProperties: ConfigProperties
) : Job {
    private val preprocessorWebClient = WebClient.create(configProperties.preprocessorUrl)

    @Suppress("MagicNumber")
    override fun execute(context: JobExecutionContext?) {
        logger.info("Running job $jobKey")
        preprocessorWebClient.post()
            .uri("/uploadStandardTestSuite")
            .retrieve()
            .toBodilessEntity()
            .block(Duration.ofSeconds(10))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateJob::class.java)
        val jobKey = JobKey(UpdateJob::class.simpleName)
    }
}
