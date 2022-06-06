/**
 * Support for scheduling updates of standard suites
 */

package com.saveourtool.save.backend.scheduling

import com.saveourtool.save.backend.configs.ConfigProperties
import org.quartz.CronScheduleBuilder
import org.quartz.CronTrigger
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.ScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean

@Configuration
@ConditionalOnBean(SchedulerFactoryBean::class)
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
