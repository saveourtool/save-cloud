/**
 * Support for scheduling updates of standard suites
 */

package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.domain.Python
import com.saveourtool.save.domain.Sdk
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.time.Duration
import javax.annotation.PostConstruct

/**
 * a [Job] that commands preprocessor to update standard test suites
 */
class UpdateJob(
    configProperties: ConfigProperties
) : Job {
    private val preprocessorWebClient = WebClient.create(configProperties.preprocessorUrl)

    @Suppress("MagicNumber")
    override fun execute(context: JobExecutionContext?) {
        preprocessorWebClient.post()
            .uri("/uploadStandardTestSuite")
            .retrieve()
            .toBodilessEntity()
            .block(Duration.ofSeconds(10))
    }
}

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

/**
 * A component that is capable of scheduling [UpdateJob]
 */
@Service
@Profile("prod")
class StandardSuitesUpdateScheduler(
    private val scheduler: Scheduler,
    configProperties: ConfigProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jobDetail = JobBuilder.newJob(UpdateJob::class.java)
        .storeDurably()
        .withIdentity(jobName)
        .withDescription("Update standard test suites in preprocessor")
        .build()
    private val trigger = TriggerBuilder.newTrigger()
        .withSchedule(
            CronScheduleBuilder.cronSchedule(configProperties.standardSuitesUpdateCron)
        )
        .build()

    /**
     * @return when the job will be executed for the first time
     */
    @PostConstruct
    fun schedule() {
        if (!scheduler.checkExists(jobDetail.key)) {
            logger.info("Scheduling job [$jobDetail], because it didn't exist.")
            scheduler.scheduleJob(jobDetail, trigger)
        } else {
            val triggers = scheduler.getTriggersOfJob(jobDetail.key)
            if (triggers.size != 1) {
                logger.warn("Job [$jobDetail] has multiple triggers: $triggers. Will drop them and reschedule.")
                triggers.forEach { scheduler.unscheduleJob(it.key) }
                scheduler.scheduleJob(jobDetail, trigger)
            } else {
                val oldTrigger = triggers.single()
                logger.info("Rescheduling job [$jobDetail] from [$oldTrigger] to [$trigger]")
                scheduler.rescheduleJob(oldTrigger.key, trigger)
            }
        }
    }

    companion object {
        internal val jobName = UpdateJob::class.simpleName
    }
}
