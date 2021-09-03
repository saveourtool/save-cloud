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
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import java.util.Date
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
            .block(Duration.ofSeconds(5))
    }
}

/**
 * A component that is capable of scheduling [UpdateJob]
 */
@Service
class StandardSuitesUpdateScheduler(
    private val scheduler: Scheduler,
) {
    private val jobDetail = JobBuilder.newJob(UpdateJob::class.java)
        .storeDurably()
        .withIdentity(jobName)
        .withDescription("Update standard test suites in preprocessor")
        .build()
    private val trigger = TriggerBuilder.newTrigger()
        .withSchedule(
            CronScheduleBuilder.cronSchedule("0 0 */1 * * ?")
        )
        .build()

    /**
     * @return when the job will be executed for the first time
     */
    @PostConstruct
    fun schedule(): Date = scheduler.scheduleJob(jobDetail, trigger)

    companion object {
        internal val jobName = UpdateJob::class.simpleName
    }
}
