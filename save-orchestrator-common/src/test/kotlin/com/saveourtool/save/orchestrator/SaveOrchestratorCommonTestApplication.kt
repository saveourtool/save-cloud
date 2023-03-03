package com.saveourtool.save.orchestrator

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.*
import org.springframework.test.context.ActiveProfiles

@SpringBootApplication
@Import(SaveOrchestratorCommonConfiguration::class)
@ActiveProfiles("test")
class SaveOrchestratorCommonTestApplication {
    /**
     * @return test bean for [MeterRegistry]
     */
    @Bean
    fun meterRegistry(): MeterRegistry = CompositeMeterRegistry()
}
