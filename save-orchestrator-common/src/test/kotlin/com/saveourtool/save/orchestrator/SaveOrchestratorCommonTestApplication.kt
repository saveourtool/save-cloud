package com.saveourtool.save.orchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotatedTypeMetadata

@SpringBootApplication
@Import(SaveOrchestratorCommonConfiguration::class)
class SaveOrchestratorCommonTestApplication {
    /**
     * Configuration for tests on Windows
     */
    @Configuration
    @Conditional(OnWindowsCondition::class)
    @Order(Ordered.LOWEST_PRECEDENCE)
    @PropertySource("classpath:META-INF/save-orchestrator-common/application-docker-tcp.properties")
    class Win

    /**
     * Condition that checks that current OS is Windows-based
     */
    class OnWindowsCondition : Condition {
        override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
                context.environment
                    .getProperty("os.name")
                    ?.contains("Win")
                    ?: false
    }
}
