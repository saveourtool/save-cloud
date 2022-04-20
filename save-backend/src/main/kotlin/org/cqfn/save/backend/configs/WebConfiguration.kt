package org.cqfn.save.backend.configs

import org.cqfn.save.backend.utils.secondsToLocalDateTime
import org.cqfn.save.backend.utils.toInstant
import org.cqfn.save.core.utils.runIf
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.util.StringUtils
import org.springframework.util.StringUtils.getFilenameExtension
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

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
    @Order(2)
    fun staticResourceRouter() = router {
        GET("/{*resourcePath}") {
            val resourcePath = it.pathVariable("resourcePath").runIf({ startsWith("/") }) { println("/"); drop(1) }
            val resource = ClassPathResource("static/$resourcePath")
            val cacheControl: (ServerResponse.BodyBuilder) -> ServerResponse.BodyBuilder = when (getFilenameExtension(resource.filename)) {
                "js" -> { b -> b.cacheControl(10.minutes) { cachePublic() }.lastModified(resource.lastModified().toInstant()) }
                "css" -> { b -> b.cacheControl(10.minutes) { cachePublic() } }
                else -> { b -> b.cacheControl(CacheControl.noCache()) }
            }
            ok().run(cacheControl)
                .bodyValue(resource)
        }
    }

    /**
     * @return a router with router for avatars that sets `Cache-Control` header
     */
    @Bean
    fun staticImageResourceRouter() = router {
        GET("/api/avatar/{*resourcePath}") {
            val resourcePath = it.pathVariable("resourcePath")
            val resource = FileSystemResource("${configProperties.fileStorage.location}/images/avatars/$resourcePath")
            ok().cacheControl(150.days) { cachePublic() }
                .lastModified(resource.lastModified().toInstant())
                .bodyValue(resource)
        }
        resources("/api/avatar/users/**", FileSystemResource("${configProperties.fileStorage.location}/images/avatars/users/"))
    }

    /**
     * @param indexPage resource for index.html
     * @param errorPage resource for error.html
     * @return router bean
     */
    @Bean
    @Order(1)
    fun indexRouter(
        @Value("classpath:/static/index.html") indexPage: Resource,
        @Value("classpath:/static/error.html") errorPage: Resource,
    ) = router {
        GET("/") {
            ok().header("Content-Type", "text/html; charset=utf8")
                .cacheControl(60.minutes) { cachePublic() }
                .bodyValue(indexPage)
        }

        GET("/error") {
            ok().header("Content-Type", "text/html; charset=utf8")
                .cacheControl(60.minutes) { cachePublic() }
                .bodyValue(errorPage)
        }
    }

    private fun ServerResponse.BodyBuilder.cacheControl(duration: kotlin.time.Duration, cacheControlCustomizer: CacheControl.() -> CacheControl = { this }): ServerResponse.BodyBuilder {
        return cacheControl(CacheControl.maxAge(duration.toJavaDuration()).run(cacheControlCustomizer))
    }
}
