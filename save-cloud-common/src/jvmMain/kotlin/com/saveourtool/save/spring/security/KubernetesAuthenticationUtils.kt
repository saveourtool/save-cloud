package com.saveourtool.save.spring.security

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import io.fabric8.kubernetes.api.model.authentication.TokenReview
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.utils.Serialization
import org.intellij.lang.annotations.Language
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

@Configuration
@Import(ServiceAccountTokenExtractorConverter::class, ServiceAccountAuthenticatingManager::class)
open class KubernetesAuthenticationUtils

@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountTokenExtractorConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.justOrEmpty(
            exchange.request.headers["X-Service-Account-Token"]?.firstOrNull()
        ).map { token ->
            logger.debug { "Starting to process `X-Service-Account-Token` of an incoming request" }
            PreAuthenticatedAuthenticationToken("TokenSupplier", token)
        }
    }

    private val logger = getLogger<ServiceAccountTokenExtractorConverter>()
}

@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountAuthenticatingManager(
    private val kubernetesClient: KubernetesClient,
) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        return authentication.toMono()
            .filter { it is PreAuthenticatedAuthenticationToken }
            .map {
                val token = it.credentials
                @Language("yaml")
                val tokenReview = """
                    |apiVersion: authentication.k8s.io/v1
                    |kind: TokenReview
                    |metadata:
                    |  name: service-account-validity-check
                    |  namespace: ${kubernetesClient.namespace}
                    |spec:
                    |  token: $token
                """.trimMargin()
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
                with (authentication) {
                    UsernamePasswordAuthenticationToken.authenticated(principal, credentials, authorities)
                }
            }
            .switchIfEmpty {
                Mono.error { BadCredentialsException("Invalid token") }
            }
    }

    private val logger = getLogger<ServiceAccountAuthenticatingManager>()
}
