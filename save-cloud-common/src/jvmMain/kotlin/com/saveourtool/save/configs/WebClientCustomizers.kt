/**
 * Customization for spring `WebClient`
 */

package com.saveourtool.save.configs

import com.saveourtool.save.spring.security.SA_HEADER_NAME
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient

import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

import kotlin.io.path.readText

/**
 * A configuration class that can be used to import all related [WebClientCustomizer] beans.
 */
@Component
class WebClientCustomizers {
    @Bean
    @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
    @Suppress("MISSING_KDOC_ON_FUNCTION", "MISSING_KDOC_CLASS_ELEMENTS")
    fun serviceAccountTokenHeaderWebClientCustomizer() = ServiceAccountTokenHeaderWebClientCustomizer()
}

/**
 * A [WebClientCustomizer] that appends Kubernetes' ServiceAccount token as a custom header.
 */
class ServiceAccountTokenHeaderWebClientCustomizer : WebClientCustomizer {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<ServiceAccountTokenHeaderWebClientCustomizer>()
    private val wrapper = ExpiringValueWrapper(Duration.ofMinutes(5)) {
        val token = Path.of("/var/run/secrets/tokens/service-account-projected-token").readText()
        token
    }

    override fun customize(builder: WebClient.Builder) {
        builder.filter { request, next ->
            val token = wrapper.getValue()
            logger.debug { "Appending `$SA_HEADER_NAME` header to the request ${request.method()} to ${request.url()}" }
            ClientRequest.from(request)
                .header(SA_HEADER_NAME, token)
                .build()
                .let(next::exchange)
        }
    }
}

/**
 * A wrapper around a value of type [T] that caches it for [expirationTimeMillis] and then recalculates
 * using [valueGetter]
 *
 * @param expirationTime value expiration time
 * @property valueGetter a function to calculate the value of type [T]
 */
class ExpiringValueWrapper<T : Any>(
    expirationTime: Duration,
    private val valueGetter: () -> T,
) {
    private val expirationTimeMillis = expirationTime.toMillis()
    private val lastUpdateTimeMillis = AtomicLong(0)
    private val value: AtomicReference<T> = AtomicReference()

    /**
     * @return cached value or refreshes the value and returns it
     */
    fun getValue(): T {
        val current = System.currentTimeMillis()
        if (current - lastUpdateTimeMillis.get() > expirationTimeMillis) {
            value.lazySet(valueGetter())
            lastUpdateTimeMillis.lazySet(current)
        }
        return value.get()
    }
}
