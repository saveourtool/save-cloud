package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.ConfigProperties
import com.saveourtool.save.orchestrator.utils.ContainersCollection
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@Configuration
@ComponentScan(
    basePackageClasses = [SaveOrchestratorCommonConfiguration::class],
)
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
@PropertySource("classpath:META-INF/save-orchestrator-common/application.properties")
class SaveOrchestratorCommonConfiguration {
    fun containersCollection(configProperties: ConfigProperties): ContainersCollection {
        return ContainersCollection(configProperties.agentsHeartBeatTimeoutMillis)
    }
}
