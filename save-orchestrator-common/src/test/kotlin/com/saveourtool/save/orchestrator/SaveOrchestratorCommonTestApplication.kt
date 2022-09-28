package com.saveourtool.save.orchestrator

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.*

@SpringBootApplication
@Import(SaveOrchestratorCommonConfiguration::class)
class SaveOrchestratorCommonTestApplication {
    /**
     * @return test bean for [MeterRegistry]
     */
    @Bean
    fun meterRegistry(): MeterRegistry = CompositeMeterRegistry()
}
