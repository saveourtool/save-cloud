package com.saveourtool.save.backend.utils

import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import io.fabric8.kubernetes.api.model.authentication.TokenReview
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.utils.Serialization
import org.intellij.lang.annotations.Language
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform
import org.springframework.boot.cloud.CloudPlatform
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountTokenExtractorConverter : ServerAuthenticationConverter {
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        return Mono.justOrEmpty(
            exchange.request.headers["X-Service-Account-Token"]?.firstOrNull()
        ).map { token ->
            PreAuthenticatedAuthenticationToken("TokenSupplier", token)
        }
    }
}

@Component
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
class ServiceAccountAuthenticatingManager(
//    val kubernetesClient: ApiClient,
    val kubernetesClient: KubernetesClient,
) : ReactiveAuthenticationManager {
    override fun authenticate(authentication: Authentication): Mono<Authentication> {
        val token = (authentication as PreAuthenticatedAuthenticationToken).credentials
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
/*        val tokenReview = V1TokenReview().apply {
            spec = V1TokenReviewSpec().apply {
                setToken(token)
            }
        }
        AuthenticationV1Api(kubernetesClient).createTokenReview(tokenReview)*/
        logger.debug {
            "Will create k8s resource from the following YAML:\n${tokenReview.prependIndent("    ")}"
        }
        val response = kubernetesClient.resource(tokenReview).createOrReplace() as TokenReview
        logger.debug {
            "Got the following response from the API server:\n${Serialization.yamlMapper().writeValueAsString(response).prependIndent("    ")}"
        }
        authentication.isAuthenticated = response.status.error == null && response.status.authenticated
        return Mono.just(authentication)
    }

    private val logger = getLogger<ServiceAccountAuthenticatingManager>()
}
