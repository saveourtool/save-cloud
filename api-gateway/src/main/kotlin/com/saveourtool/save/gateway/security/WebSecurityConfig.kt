/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.gateway.security

import com.saveourtool.save.authservice.utils.*
import com.saveourtool.save.gateway.config.ConfigurationProperties
import com.saveourtool.save.gateway.service.BackendService
import com.saveourtool.save.gateway.utils.StoringServerAuthenticationSuccessHandler
import com.saveourtool.save.v1

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono

/**
 * List of endpoints allowed for users that do not have ACTIVE status
 */
internal val allowedForInactiveEndpoints = listOf(
    "/api/$v1/users/*",
)

@EnableWebFluxSecurity
@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION",
    "TOO_LONG_FUNCTION",
    "TOO_MANY_LINES_IN_LAMBDA",
)
class WebSecurityConfig(
    private val configurationProperties: ConfigurationProperties,
    private val backendService: BackendService,
) {
    @Bean
    @Order(1)
    @Suppress("LongMethod")
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.securityMatcher(
        // access to actuator is managed separately
        matchAllExcludingActuator()
    )
        .authorizeExchange { authorizeExchangeSpec ->
            // this is default data that is required by FE to operate properly
            authorizeExchangeSpec.pathMatchers(
                // FixMe: Extract into properties
                "/",
                "/login", "/logout",
                "/sec/oauth-providers", "/sec/user",
                "/error",
                "/neo4j/**",
            )
                .permitAll()
                // all requests to backend are permitted on gateway, if user agent is authenticated in gateway or doesn't have
                // any authentication data at all.
                // backend returns 401 for those endpoints that require authentication
                .pathMatchers(*allowedForInactiveEndpoints.toTypedArray()).access(::defaultAuthorizationDecision)
                .pathMatchers("/api/**").access { authorization, authorizationContext ->
                    authorization.flatMap { backendService.findByName(it.name) }
                        .filter { it.isActive() }
                        .flatMap { defaultAuthorizationDecision(authorization, authorizationContext) }
                        .switchIfEmpty { AuthorizationDecision(false).toMono() }
                }
                // resources for frontend
                .pathMatchers("/*.html", "/*.js*", "/*.css", "/img/**", "/*.ico", "/*.png", "/particles.json")
                .permitAll()
        }
        .run {
            authorizeExchange()
                // api-gateway forwards everything to save-frontend now
                .pathMatchers("/**")
                .permitAll()
        }
        .and()
        .run {
            // FixMe: Properly support CSRF protection https://github.com/saveourtool/save-cloud/issues/34
            csrf().disable()
        }
        .exceptionHandling {
            it.authenticationEntryPoint(
                // return 401 for unauthorized requests instead of redirect to log-in
                HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
            )
        }
        .oauth2Login {
            it.authenticationSuccessHandler(
                DelegatingServerAuthenticationSuccessHandler(
                    StoringServerAuthenticationSuccessHandler(backendService),
                    RedirectServerAuthenticationSuccessHandler("/"),
                )
            )
            it.authenticationFailureHandler(
                RedirectServerAuthenticationFailureHandler("/error")
            )
        }
        .httpBasic { httpBasicSpec ->
            // Authenticate by comparing received basic credentials with existing one from DB
            httpBasicSpec.authenticationManager(
                UserDetailsRepositoryReactiveAuthenticationManager { username ->
                    backendService.findByName(username).cast<UserDetails>()
                }
            )
        }
        .logout {
            // fixme: when frontend can handle logout without reloading, use `RedirectServerLogoutSuccessHandler` here
            it.logoutSuccessHandler(HttpStatusReturningServerLogoutSuccessHandler(HttpStatus.OK))
        }
        .build()

    @Bean
    @Order(2)
    @Suppress("AVOID_NULL_CHECKS")
    fun actuatorSecurityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        // Allow access to actuator only from a set of addresses or subnets, without any additional checks.
        authorizeExchange()
            .matchers(
                AndServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/actuator", "/actuator/**"),
                    ServerWebExchangeMatcher { request ->
                        val isKnownActuatorConsumer = configurationProperties.isKnownActuatorConsumer(
                            request.request.remoteAddress?.address
                        )
                        if (isKnownActuatorConsumer) MatchResult.match() else MatchResult.notMatch()
                    }
                )
            )
            .permitAll()
    }
        .and().build()

    private fun matchAllExcludingActuator() = AndServerWebExchangeMatcher(
        ServerWebExchangeMatchers.pathMatchers("/**"),
        NegatedServerWebExchangeMatcher(
            ServerWebExchangeMatchers.pathMatchers("/actuator", "/actuator/**")
        )
    )
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
