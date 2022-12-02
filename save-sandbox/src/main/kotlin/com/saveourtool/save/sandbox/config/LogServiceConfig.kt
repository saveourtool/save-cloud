package com.saveourtool.save.sandbox.config

import com.saveourtool.save.service.LogService
import com.saveourtool.save.service.LokiLogService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring's configuration to configure [LogService]
 */
@Configuration
class LogServiceConfig {
    /**
     * @param configProperties
     * @return [LokiLogService] when properties *sandbox.loki.** are set
     */
    @Bean
    fun logService(
        configProperties: ConfigProperties,
    ): LogService = LokiLogService.createOrStub(configProperties.lokiConfig)
}
