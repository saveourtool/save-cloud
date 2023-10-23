package com.saveourtool.save.cosv

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * Configuration for Vulnerability
 */
@Configuration
@ComponentScan
@EnableJpaRepositories(basePackages = ["com.saveourtool.save.cosv.repository"])
class CosvConfiguration
