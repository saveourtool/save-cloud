package com.saveourtool.save.configs

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.readText

@Component
class WebClientCustomizers {
    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
    fun serviceAccountTokenHeaderWebClientCustomizer() = ServiceAccountTokenHeaderWebClientCustomizer()

    private val logger = getLogger<WebClientCustomizers>()
}

class ServiceAccountTokenHeaderWebClientCustomizer : WebClientCustomizer {
    private val wrapper = ExpiringValueWrapper(Duration.ofMinutes(5)) {
        val token = Path.of("/var/run/secrets/tokens/service-account-projected-token").readText()
        token
    }

    override fun customize(builder: WebClient.Builder) {
        builder.filter { request, next ->
            val token = wrapper.getValue()
            logger.debug { "Appending `X-Service-Account-Token` header to the request ${request.method()} to ${request.url()}" }
            ClientRequest.from(request)
                .header("X-Service-Account-Token", token)
                .build()
                .let(next::exchange)
        }
    }

    private val logger = getLogger<ServiceAccountTokenHeaderWebClientCustomizer>()
}

class ExpiringValueWrapper<T : Any>(
    expirationTime: Duration,
    private val valueGetter: () -> T,
) {
    private val expirationTimeMillis = expirationTime.toMillis()
    private val lastUpdateTimeMillis = AtomicLong(0)
    private val value = AtomicReference<T>()

    fun getValue(): T {
        val current = System.currentTimeMillis()
        if (current - lastUpdateTimeMillis.get() > expirationTimeMillis) {
            value.lazySet(valueGetter())
            lastUpdateTimeMillis.lazySet(current)
        }
        return value.get()
    }
}
