package com.saveourtool.save.backend.configs

import com.saveourtool.save.backend.storage.AvatarKey
import com.saveourtool.save.backend.storage.AvatarStorage
import com.saveourtool.save.v1

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
    private val avatarStorage: AvatarStorage,
) {
    /**
     * @return a router with routes for avatars that set `Cache-Control` header
     */
    @Bean
    fun staticStorageResourceRouter() = router {
        cacheableAvatar(
            "/api/$v1/avatar/{*resourcePath}",
        )
        cacheableAvatar(
            "/api/$v1/avatar/users/{*resourcePath}",
        )
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

    private fun RouterFunctionDsl.cacheableAvatar(
        pattern: String,
    ) = cacheableResource(pattern) { AvatarKey(it) }

    private fun RouterFunctionDsl.cacheableResource(
        pattern: String,
        avatarKeyExtractor: (relativePath: String) -> AvatarKey,
    ) = GET(pattern) { request ->
        val resourcePath = request.pathVariable("resourcePath")
        val avatarKey = avatarKeyExtractor(resourcePath)
        avatarStorage.doesExist(avatarKey)
            .flatMap {
                avatarStorage.lastModified(avatarKey)
            }
            .flatMap { lastModified ->
                ok().cacheControl(longExpirationTime) { cachePublic() }
                    .lastModified(lastModified)
                    .bodyValue(avatarStorage.download(avatarKey))
            }
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
