package com.saveourtool.save.orchestrator

import com.saveourtool.save.orchestrator.config.ConfigProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * An entrypoint for spring boot for save-orchestrator
 */
@ComponentScan(basePackageClasses = [SaveOrchestratorCommonConfiguration::class])
@EnableConfigurationProperties(ConfigProperties::class)
@EnableScheduling
class SaveOrchestratorCommonConfiguration
