package com.saveourtool.save.gateway.utils

import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.User
import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.info.UserStatus

import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import reactor.core.publisher.Mono

/**
 * [ServerAuthenticationSuccessHandler] that sends user data to backend on successful login
 */
class StoringServerAuthenticationSuccessHandler(
    private val backendService: BackendService,
) : ServerAuthenticationSuccessHandler {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        logger.info("Authenticated user ${authentication.userName()} with authentication type ${authentication::class}, will send data to backend")

        val user = authentication.toUser().apply {
            // https://github.com/saveourtool/save-cloud/issues/583
            // fixme: this sets a default role for a new user with minimal scope, however this way we discard existing role
            // from authentication provider. In the future we may want to use this information and have a mapping of existing
            // roles to save-cloud roles.
            role = Role.VIEWER.asSpringSecurityRole()
        }

        return backendService.createNew(user)
    }
}

/**
 * @return [User] with data from this [Authentication]
 */
private fun Authentication.toUser(): User = User(
    userName(),
    null,
    authorities.joinToString(",") { it.authority },
    toIdentitySource(),
    null,
    status = UserStatus.CREATED,
    originalLogins = emptyList(),
)
