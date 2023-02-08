/**
 * It contains [org.springframework.context.annotation.PropertySource] for profiles
 */

package com.saveourtool.save.orchestrator.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.PropertySource

/**
 * Configuration for profile 'container-desktop'
 */
@Configuration
@Profile("container-desktop")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-container-desktop.properties")
class SaveOrchestratorCommonPropertySourceConfigurationContainerDesktop

/**
 * Configuration for profile 'dev'
 */
@Configuration
@Profile("dev")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-dev.properties")
class SaveOrchestratorCommonPropertySourceConfigurationDev

/**
 * Configuration for profile 'docker-tcp'
 */
@Configuration
@Profile("docker-tcp")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
class SaveOrchestratorCommonPropertySourceConfigurationDockerTcp

/**
 * Configuration enabled when `docker-tcp` is disabled
 */
@Configuration
@Profile("!docker-tcp")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-docker-socket.properties")
class SaveOrchestratorCommonPropertySourceConfigurationDockerSocket

/**
 * Configuration for profile 'kafka'
 */
@Configuration
@Profile("kafka")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-kafka.properties")
class SaveOrchestratorCommonPropertySourceConfigurationKafka

/**
 * Configuration for profile 'mac'
 */
@Configuration
@Profile("mac")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-mac.properties")
class SaveOrchestratorCommonPropertySourceConfigurationMac

/**
 * Configuration for profile 'swagger'
 */
@Configuration
@Profile("swagger")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-swagger.properties")
class SaveOrchestratorCommonPropertySourceConfigurationSwagger

/**
 * Configuration for profile 'win'
 */
@Configuration
@Profile("win")
@PropertySource("classpath:META-INF/save-orchestrator-common/application-win.properties")
class SaveOrchestratorCommonPropertySourceConfigurationWin
