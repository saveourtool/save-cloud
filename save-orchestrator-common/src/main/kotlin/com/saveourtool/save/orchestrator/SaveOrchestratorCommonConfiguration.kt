package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@Configuration
@ComponentScan(basePackageClasses = [SaveOrchestratorCommonConfiguration::class])
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
@PropertySource("classpath:META-INF/save-orchestrator-common/application.properties")
class SaveOrchestratorCommonConfiguration {
    /**
     * Configuration for profile 'dev'
     */
    @Configuration
    @Profile("dev")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-dev.properties")
    class Dev

    /**
     * Configuration for profile 'docker-tcp'
     */
    @Configuration
    @Profile("docker-tcp")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
    class DockerTcp

    /**
     * Configuration for profile 'kafka'
     */
    @Configuration
    @Profile("kafka")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-kafka.properties")
    class Kafka

    /**
     * Configuration for profile 'mac'
     */
    @Configuration
    @Profile("mac")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-mac.properties")
    class Mac

    /**
     * Configuration for profile 'swagger'
     */
    @Configuration
    @Profile("swagger")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-swagger.properties")
    class Swagger

    /**
     * Configuration for profile 'win'
     */
    @Configuration
    @Profile("win")
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-win.properties")
    class Win
}
