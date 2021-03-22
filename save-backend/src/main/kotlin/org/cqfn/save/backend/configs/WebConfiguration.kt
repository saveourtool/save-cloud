package org.cqfn.save.backend.configs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.router

/**
 * Configuration class that enables serving static resources
 */
@Configuration
class WebConfiguration(
    val configProperties: ConfigProperties,
) {
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
        GET("/") {
            ok().contentType(TEXT_HTML).bodyValue(html)
        }
    }

    /**
     * @param builder
     * @return web client
     */
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient =
            builder
                .baseUrl(configProperties.preprocessorUrl)
                .build()
}
