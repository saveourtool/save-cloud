package org.cqfn.save.backend.configs

import org.cqfn.save.backend.utils.toInstant
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.util.StringUtils.getFilenameExtension
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
    private val buildProperties: BuildProperties,
) {
    /**
     * @return a router bean
     */
    @Bean
    fun staticResourceRouter() = router {
        path("/{resourcePath:.*\\.(?:js|css|html)}") {
            val resourcePath = it.pathVariable("resourcePath")
            val resource = ClassPathResource("static/$resourcePath")
            val cacheControl: (ServerResponse.BodyBuilder) -> ServerResponse.BodyBuilder = when (getFilenameExtension(resource.filename)) {
                "js", "css", "html" -> { b -> b.cacheControl(10.minutes) { cachePublic() } }
                else -> { b -> b.cacheControl(CacheControl.noCache()) }
            }
            ok().run(cacheControl)
                .lastModified(buildProperties.time)
                .bodyValue(resource)
        }
        // fallback for other resources
        resources("/**", ClassPathResource("static/"))
    }

    /**
     * @return a router with router for avatars that sets `Cache-Control` header
     */
    @Bean
    fun staticImageResourceRouter() = router {
        cacheableFsResource(
            "/api/avatar/{*resourcePath}",
            "${configProperties.fileStorage.location}/images/avatars",
        )
        cacheableFsResource(
            "/api/avatar/users/{*resourcePath}",
            "${configProperties.fileStorage.location}/images/avatars/users",
        )
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
            ok().header("Content-Type", "text/html; charset=utf8")
                .cacheControl(10.minutes) { cachePublic() }
                .lastModified(buildProperties.time)
                .bodyValue(indexPage)
        }

        GET("/error") {
            ok().header("Content-Type", "text/html; charset=utf8")
                .cacheControl(10.minutes) { cachePublic() }
                .lastModified(buildProperties.time)
                .bodyValue(errorPage)
        }
    }

    private fun RouterFunctionDsl.cacheableFsResource(
        pattern: String,
        basePath: String,
    ) = GET(pattern) {
        val resourcePath = it.pathVariable("resourcePath")
        val resource = FileSystemResource("$basePath/$resourcePath")
        ok().cacheControl(150.days) { cachePublic() }
            .lastModified(resource.lastModified().toInstant())
            .bodyValue(resource)
    }

    private fun ServerResponse.BodyBuilder.cacheControl(duration: kotlin.time.Duration, cacheControlCustomizer: CacheControl.() -> CacheControl = { this }): ServerResponse.BodyBuilder = cacheControl(CacheControl.maxAge(duration.toJavaDuration()).run(cacheControlCustomizer))
}
