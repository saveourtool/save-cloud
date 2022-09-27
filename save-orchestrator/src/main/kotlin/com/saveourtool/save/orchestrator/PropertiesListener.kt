package com.saveourtool.save.orchestrator

import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.stereotype.Component

@Component
class PropertiesListener {

    fun handleContextRefreshed(event: ContextRefreshedEvent) {
        event.applicationContext.environment
            .let { it as ConfigurableEnvironment }
            .propertySources
            .forEach {
                print("${it.name} == ${it.source}")
            }
    }
}