/**
 * Support for scheduling updates of standard suites
 */

package org.cqfn.save.backend.scheduling

import org.cqfn.save.backend.configs.ConfigProperties
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
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

/**
 * A component that is capable of scheduling [UpdateJob]
 */
@Service
@Profile("automatic-updates")
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
            logger.info("Scheduling job $jobDetail, because it didn't exist.")
            scheduler.scheduleJob(jobDetail, trigger)
        } else {
            val triggers = scheduler.getTriggersOfJob(jobDetail.key)
            if (triggers.size != 1) {
                logger.warn("Job $jobDetail has multiple triggers: $triggers. Will drop them and reschedule.")
                triggers.forEach { scheduler.unscheduleJob(it.key) }
                scheduler.scheduleJob(jobDetail, trigger)
            } else {
                val oldTrigger = triggers.single()
                logger.info("Rescheduling job $jobDetail from $oldTrigger to $trigger")
                scheduler.rescheduleJob(oldTrigger.key, trigger)
            }
        }
    }

    companion object {
        internal val jobName = UpdateJob::class.simpleName
    }
}
