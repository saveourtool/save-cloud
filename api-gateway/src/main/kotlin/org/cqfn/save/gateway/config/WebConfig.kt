package org.cqfn.save.gateway.config

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.security.jackson2.CoreJackson2Module

@Configuration
class WebConfig {
    @Bean
    fun jackson2ObjectMapperBuilderCustomizer() = Jackson2ObjectMapperBuilderCustomizer { jacksonObjectMapperBuilder: Jackson2ObjectMapperBuilder ->
        jacksonObjectMapperBuilder
            .modules(CoreJackson2Module())
        //.mixIn(TestStatus::class.java, TestStatusMixin::class.java)
        //.mixIn(User::class.java, UserMixin::class.java)
    }
}