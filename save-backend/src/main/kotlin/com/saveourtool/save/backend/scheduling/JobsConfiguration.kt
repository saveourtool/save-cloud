/**
 * Support for scheduling updates of standard suites
 */

package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import org.quartz.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("prod")
@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "KDOC_NO_EMPTY_TAGS",
    "MISSING_KDOC_ON_FUNCTION",
    "MISSING_KDOC_CLASS_ELEMENTS"
)
class JobsConfiguration {
    @Bean
    fun updateJobDetail(): JobDetail = JobBuilder.newJob(UpdateJob::class.java)
        .storeDurably()
        .withIdentity(UpdateJob.jobKey)
        .withDescription("Update standard test suites in preprocessor")
        .build()

    @Bean
    fun baseImageBuildingJobDetail(): JobDetail = JobBuilder.newJob(BaseImageBuildingJob::class.java)
        .storeDurably()
        .withIdentity(BaseImageBuildingJob.jobKey)
        .withDescription("Build base images for test execution for different SDKs")
        .build()

    @Bean
    fun updateJobTrigger(configProperties: ConfigProperties): CronTrigger = scheduledTrigger(
        UpdateJob.jobKey,
        CronScheduleBuilder.cronSchedule(configProperties.scheduling.standardSuitesUpdateCron)
    )

    @Bean
    fun buildBaseImageJobTrigger(configProperties: ConfigProperties): CronTrigger = scheduledTrigger(
        BaseImageBuildingJob.jobKey,
        CronScheduleBuilder.cronSchedule(configProperties.scheduling.baseImagesBuildCron)
    )
}

private fun <T : Trigger> scheduledTrigger(jobKey: JobKey, scheduleBuilder: ScheduleBuilder<T>): T = TriggerBuilder.newTrigger()
    .withSchedule(scheduleBuilder)
    .forJob(jobKey)
    .build()
