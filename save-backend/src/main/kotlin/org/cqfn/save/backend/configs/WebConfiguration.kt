package org.cqfn.save.backend.configs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.router
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Configuration class that enables serving static resources
 */
@Configuration
class WebConfiguration {
    /**
     * @return a rotuer bean
     */
    @Bean
    fun staticResourceRouter() = router {
        resources("/**", ClassPathResource("static/"))
    }

    /**
     * @param html requested resource
     * @return router bean
     */
    @Bean
    fun indexRouter(@Value("classpath:/static/index.html") html: Resource) = router {
        GET("/") { request ->
            request.principal().flatMap {
                // if user is logged in, show `index.html`
                ok().header("Content-Type", "text/html; charset=utf8").bodyValue(html)
            }.switchIfEmpty {
                // if not, redirect to the login page
                status(HttpStatus.FOUND).header("Location", "/login").build()
            }
        }
    }
}
