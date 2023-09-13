package com.saveourtool.save.gateway.controller

import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.info.OauthProviderInfo
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono

/**
 * Controller that returns various public information
 */
@RestController
@RequestMapping("/sec")
class SecurityInfoController(
    private val clientRegistrationRepository: InMemoryReactiveClientRegistrationRepository,
    private val backendService: BackendService,
) {
    /**
     * @return a list of [OauthProviderInfo] for all configured providers
     */
    @GetMapping("/oauth-providers")
    fun listOauthProviders() = clientRegistrationRepository.map {
        OauthProviderInfo(
            it.registrationId,
            // Default authorization link format,
            // see https://docs.spring.io/spring-security/reference/reactive/oauth2/login/advanced.html#webflux-oauth2-login-advanced-login-page
            "/oauth2/authorization/${it.registrationId}",
        )
    }

    /**
     * Endpoint that provides the information about the current logged-in user (powered by spring security and OAUTH)
     *
     * @param authentication
     * @param session
     * @return user information
     */
    @GetMapping("/user")
    fun currentUserName(
        authentication: Authentication?,
        session: WebSession,
    ): Mono<String> = authentication
        ?.let { principal -> backendService.findByPrincipal(principal, session).map { it.name } }
        ?: Mono.empty()
}
