package org.cqfn.save.backend.configs

import org.cqfn.save.v1
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.web.reactive.function.server.router

/**
 * Configuration class that enables serving static resources
 */
@Configuration
class WebConfiguration(
    private val configProperties: ConfigProperties
) {
    /**
     * @return a router bean
     */
    @Bean
    fun staticResourceRouter() = router {
        resources("/**", ClassPathResource("static/"))
    }

    /**
     * @return a router for image bean
     */
    @Bean
    fun staticImageResourceRouter() = router {
        resources("/api/$v1/avatar/**", FileSystemResource("${configProperties.fileStorage.location}/images/avatars/"))
        resources("/api/$v1/avatar/users/**", FileSystemResource("${configProperties.fileStorage.location}/images/avatars/users/"))
    }

    /**
     * @param indexPage resource for index.html
     * @param errorPage resource for error.html
     * @return router bean
     */
    @Bean
    fun indexRouter(
        @Value("classpath:/static/index.html") indexPage: Resource,
        @Value("classpath:/static/error.html") errorPage: Resource,
    ) = router {
        GET("/") {
            ok().header("Content-Type", "text/html; charset=utf8").bodyValue(indexPage)
        }

        GET("/error") {
            ok().header("Content-Type", "text/html; charset=utf8").bodyValue(errorPage)
        }
    }
}
