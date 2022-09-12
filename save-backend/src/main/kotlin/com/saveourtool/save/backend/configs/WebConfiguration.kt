package com.saveourtool.save.backend.configs

import com.saveourtool.save.backend.utils.toInstant
import com.saveourtool.save.v1

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.web.reactive.function.server.RouterFunctionDsl
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Configuration class that enables serving static resources
 */
@Configuration
class WebConfiguration(
    private val configProperties: ConfigProperties,
) {
    /**
     * @return a router with routes for avatars that set `Cache-Control` header
     */
    @Bean
    fun staticStorageResourceRouter() = router {
        cacheableFsResource(
            "/api/$v1/avatar/{*resourcePath}",
            "${configProperties.fileStorage.location}/images/avatars",
        )
        cacheableFsResource(
            "/api/$v1/avatar/users/{*resourcePath}",
            "${configProperties.fileStorage.location}/images/avatars/users",
        )
        resources("/api/$v1/resource/**", FileSystemResource("${configProperties.fileStorage.location}/storage/"))
    }

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

    private fun RouterFunctionDsl.cacheableFsResource(
        pattern: String,
        basePath: String
    ) = cacheableResource(pattern, basePath) { FileSystemResource(it) }

    private fun RouterFunctionDsl.cacheableResource(
        pattern: String,
        basePath: String,
        resource: (path: String) -> Resource,
    ) = GET(pattern) { request ->
        val resourcePath = request.pathVariable("resourcePath")
        val newResource = resource("$basePath/$resourcePath")
        ok().cacheControl(longExpirationTime) { cachePublic() }
            .lastModified(newResource.lastModified().toInstant())
            .bodyValue(newResource)
    }

    private fun ServerResponse.BodyBuilder.cacheControl(duration: kotlin.time.Duration,
                                                        cacheControlCustomizer: CacheControl.() -> CacheControl = { this },
    ): ServerResponse.BodyBuilder =
            cacheControl(CacheControl.maxAge(duration.toJavaDuration()).run(cacheControlCustomizer))

    companion object {
        private val shortExpirationTime = 10.minutes
        private val longExpirationTime = 150.days
    }
}
