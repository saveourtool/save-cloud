/**
 * Utilities to configure Kubernetes ServiceAccount token-based authentication in Spring Security.
 */

package com.saveourtool.save.authservice.utils

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import io.fabric8.kubernetes.api.model.authentication.TokenReview
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.utils.Serialization
import org.intellij.lang.annotations.Language
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

const val SA_HEADER_NAME = "X-Service-Account-Token"

/**
 * A Configuration class that can be used to import all related beans to set up Spring Security
 * to work with Kubernetes ServiceAccount tokens.
 */
@Configuration
@Import(
    ServiceAccountTokenExtractorConverter::class,
    ServiceAccountAuthenticatingManager::class,
)
@Suppress(
    "AVOID_USING_UTILITY_CLASS",  // Spring beans need to be declared inside `@Configuration` class.
)
class KubernetesAuthenticationUtils {
    @ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
    @Bean
    @Order(2)
    @Suppress(
        "MISSING_KDOC_CLASS_ELEMENTS",
        "MISSING_KDOC_ON_FUNCTION",
    )
    fun internalSecuredSecurityChain(
        http: ServerHttpSecurity,
        serviceAccountAuthenticatingManager: ServiceAccountAuthenticatingManager,
        serviceAccountTokenExtractorConverter: ServiceAccountTokenExtractorConverter,
    ): SecurityWebFilterChain = http.run {
        securityMatcher(
            NegatedServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/api/**", "/sandbox/api/**")
            )
        )
            .authorizeExchange()
            .pathMatchers("/actuator/**")
            // all requests to `/actuator` should be sent only from inside the cluster
            // access to this port should be controlled by a NetworkPolicy
            .permitAll()
            .pathMatchers(
                // FixMe: https://github.com/saveourtool/save-cloud/pull/1247
                "/internal/files/download-save-agent",
                "/internal/files/download-save-cli",
                "/internal/files/download",
                "/internal/files/debug-info",
                "/internal/test-suites-sources/download-snapshot-by-execution-id",
                "/internal/saveTestResult",
                "/heartbeat",
            )
            .permitAll()
            .and()
            .authorizeExchange()
            .pathMatchers("/**")
            .authenticated()
            .and()
            .serviceAccountTokenAuthentication(serviceAccountTokenExtractorConverter, serviceAccountAuthenticatingManager)
            .csrf()
            .disable()
            .logout()
            .disable()
            .formLogin()
            .disable()
            .build()
    }

    /**
     * No-op security config when not running in Kubernetes.
     * FixMe: can be removed in favor of common `WebSecurityConfig` from authService?
     */
    @ConditionalOnCloudPlatform(CloudPlatform.NONE)
    @Bean
    @Order(2)
    @Suppress(
        "MISSING_KDOC_CLASS_ELEMENTS",
        "MISSING_KDOC_ON_FUNCTION",
        "KDOC_WITHOUT_PARAM_TAG",
        "KDOC_WITHOUT_RETURN_TAG",
    )
    fun internalInsecureSecurityChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        securityMatcher(
            ServerWebExchangeMatchers.pathMatchers("/internal/**", "/actuator/**")
        )
            .authorizeExchange()
            .pathMatchers("/internal/**", "/actuator/**")
            .permitAll()
            .and()
            .csrf()
            .disable()
            .build()
    }
}

/**
 * A [ServerAuthenticationConverter] that attempts to convert a [ServerWebExchange] to an [Authentication]
 * if it encounters a SA token in [SA_HEADER_NAME] header.
 */
@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountTokenExtractorConverter : ServerAuthenticationConverter {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<ServiceAccountTokenExtractorConverter>()
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = Mono.justOrEmpty(
        exchange.request.headers[SA_HEADER_NAME]?.firstOrNull()
    ).map { token ->
        logger.debug { "Starting to process `$SA_HEADER_NAME` of an incoming request [${exchange.request.method} ${exchange.request.uri}]" }
        PreAuthenticatedAuthenticationToken("TokenSupplier", token)
    }
}

/**
 * A [ReactiveAuthenticationManager] that is intended to be used together with [ServerAuthenticationConverter].
 * Attempts to authenticate an [Authentication] validating ServiceAccount token using TokeReview API.
 */
@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountAuthenticatingManager(
    private val kubernetesClient: KubernetesClient,
) : ReactiveAuthenticationManager {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    private val logger = getLogger<ServiceAccountAuthenticatingManager>()
    override fun authenticate(authentication: Authentication): Mono<Authentication> = authentication.toMono()
        .filter { it is PreAuthenticatedAuthenticationToken }
        .map { preAuthenticatedAuthenticationToken ->
            val tokenReview = tokenReviewSpec(preAuthenticatedAuthenticationToken.credentials as String)
            logger.debug {
                "Will create k8s resource from the following YAML:\n${tokenReview.prependIndent("    ")}"
            }
            val response = kubernetesClient.resource(tokenReview).createOrReplace() as TokenReview
            logger.debug {
                "Got the following response from the API server:\n${
                    Serialization.yamlMapper().writeValueAsString(response).prependIndent("    ")
                }"
            }
            response
        }
        .filter { response ->
            val isAuthenticated = response.status.error.isNullOrEmpty() && response.status.authenticated
            logger.debug { "After the response from TokenReview, request authentication is $isAuthenticated" }
            isAuthenticated
        }
        .map<Authentication> {
            with(authentication) {
                UsernamePasswordAuthenticationToken.authenticated(principal, credentials, authorities)
            }
        }
        .switchIfEmpty {
            Mono.error { BadCredentialsException("Invalid token") }
        }

    @Language("YAML")
    private fun tokenReviewSpec(token: String): String = """
        |apiVersion: authentication.k8s.io/v1
        |kind: TokenReview
        |metadata:
        |  name: service-account-validity-check
        |  namespace: ${kubernetesClient.namespace}
        |spec:
        |  token: $token
    """.trimMargin()
}

/**
 * Configures authentication and authorization using Kubernetes ServiceAccount tokens.
 * This method requires two beans which can be imported with [KubernetesAuthenticationUtils] configuration class.
 */
@Suppress("KDOC_WITHOUT_PARAM_TAG", "KDOC_WITHOUT_RETURN_TAG")
fun ServerHttpSecurity.serviceAccountTokenAuthentication(
    serviceAccountTokenExtractorConverter: ServiceAccountTokenExtractorConverter,
    serviceAccountAuthenticatingManager: ServiceAccountAuthenticatingManager,
): ServerHttpSecurity = addFilterBefore(
    AuthenticationWebFilter(serviceAccountAuthenticatingManager).apply {
        setServerAuthenticationConverter(serviceAccountTokenExtractorConverter)
    },
    SecurityWebFiltersOrder.HTTP_BASIC
)
    .exceptionHandling {
        it.authenticationEntryPoint(
            HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
        )
    }
