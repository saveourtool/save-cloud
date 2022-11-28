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
     * @param lokiServiceUrl
     * @return [LokiLogService] when property [LOKI_SERVICE_URL] is set
     */
    @Bean
    @ConditionalOnProperty(LOKI_SERVICE_URL)
    fun logService(
        @Value(LOKI_SERVICE_URL) lokiServiceUrl: String,
    ): LogService = LokiLogService(lokiServiceUrl)

    /**
     * @return [LogService.stub] when property [LOKI_SERVICE_URL] is not set
     */
    @Bean
    @ConditionalOnProperty(LOKI_SERVICE_URL, matchIfMissing = true)
    fun stubLogService(): LogService = LogService.stub

    companion object {
        private const val LOKI_SERVICE_URL: String = "\${sandbox.loki-service-url}"
    }
}
