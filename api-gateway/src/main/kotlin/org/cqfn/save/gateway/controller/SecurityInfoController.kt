package org.cqfn.save.gateway.controller

import org.cqfn.save.info.OauthProviderInfo
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller that returns various public information
 */
@RestController
@RequestMapping("/info")
class SecurityInfoController(
    private val clientRegistrationRepository: InMemoryReactiveClientRegistrationRepository,
) {
    /**
     * @return a list of [OauthProviderInfo] for all configured providers
     */
    @GetMapping("/oauth-providers")
    fun listOauthProviders() = clientRegistrationRepository.map {
        OauthProviderInfo(
            it.registrationId,
            // Default authorization link format, see https://docs.spring.io/spring-security/reference/reactive/oauth2/login/advanced.html#webflux-oauth2-login-advanced-login-page
            "/oauth2/authorization/${it.registrationId}",
        )
    }
}
