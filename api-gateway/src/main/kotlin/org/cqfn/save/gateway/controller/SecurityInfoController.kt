package org.cqfn.save.gateway.controller

import org.cqfn.save.info.OauthProviderInfo
import org.cqfn.save.info.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * Controller that returns various public information
 */
@RestController
@RequestMapping("/sec")
class SecurityInfoController(
    private val clientRegistrationRepository: InMemoryReactiveClientRegistrationRepository,
) {

    private val logger = LoggerFactory.getLogger(SecurityInfoController::class.java)

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

    @GetMapping("/user")
    fun currentUserName(principal: Principal): UserInfo {
        logger.info(principal.javaClass.toString())

        return UserInfo((
                (principal as? OAuth2AuthenticationToken)
                    ?.principal
                    ?.attributes
                    ?.get("login") as String?
                )?: principal.name)
    }
}
