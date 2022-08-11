package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.service.TestSuitesSourceService
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobKey
import org.slf4j.LoggerFactory
import reactor.kotlin.core.publisher.toFlux
import java.time.Duration

/**
 * a [Job] that commands preprocessor to update standard test suites
 */
class UpdateJob(
    private val testSuitesSourceService: TestSuitesSourceService,
) : Job {
    @Suppress("MagicNumber")
    override fun execute(context: JobExecutionContext?) {
        logger.info("Running job $jobKey")
        testSuitesSourceService.getStandardTestSuitesSources()
            .toFlux()
            .flatMap { testSuitesSource ->
                testSuitesSourceService.fetch(testSuitesSource.toDto())
            }
            .collectList()
            .block(Duration.ofSeconds(10))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UpdateJob::class.java)
        val jobKey = JobKey(UpdateJob::class.simpleName)
    }
}
