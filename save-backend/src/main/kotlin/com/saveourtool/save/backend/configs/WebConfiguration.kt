package com.saveourtool.save.backend.configs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Configuration class that enables serving static resources
 */
@Configuration
class WebConfiguration {
    /**
     * @param errorPage resource for error.html
     * @return router bean
     */
    @Bean
    fun indexRouter(
        @Value("classpath:/error.html") errorPage: Resource,
    ) = router {
        GET("/error") {
            ok().header("Content-Type", "text/html; charset=utf8")
                .cacheControl(shortExpirationTime) { cachePublic() }
                .bodyValue(errorPage)
        }
    }

    private fun ServerResponse.BodyBuilder.cacheControl(duration: kotlin.time.Duration,
                                                        cacheControlCustomizer: CacheControl.() -> CacheControl = { this },
    ): ServerResponse.BodyBuilder =
            cacheControl(CacheControl.maxAge(duration.toJavaDuration()).run(cacheControlCustomizer))

    companion object {
        private val shortExpirationTime = 10.minutes
    }
}
