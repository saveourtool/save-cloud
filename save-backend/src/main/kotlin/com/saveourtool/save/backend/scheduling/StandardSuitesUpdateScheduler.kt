/**
 * Support for scheduling updates of standard suites
 */

package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Configuration
class JobsConfiguration {
    @Bean
    fun updateJobDetail() = JobBuilder.newJob(UpdateJob::class.java)
        .storeDurably()
        .withIdentity(StandardSuitesUpdateScheduler.jobName)
        .withDescription("Update standard test suites in preprocessor")
        .build()

    @Bean
    fun baseImageBuildingJobDetail() =  JobBuilder.newJob(BaseImageBuildingJob::class.java)
        .storeDurably()
        .withIdentity(/*TODO*/)
        .withDescription(/*TODO*/)
        .build()
}

/**
 * A component that is capable of scheduling [UpdateJob]
 */
@Service
@Profile("prod")
class StandardSuitesUpdateScheduler(
    private val scheduler: Scheduler,
    private val jobDetails: List<JobDetail>,
    configProperties: ConfigProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
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
        jobDetails.forEach { jobDetail ->
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
    }

    companion object {
        internal val jobName = UpdateJob::class.simpleName
    }
}
