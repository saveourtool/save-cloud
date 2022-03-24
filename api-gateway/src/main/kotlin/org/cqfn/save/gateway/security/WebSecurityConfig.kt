/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.gateway.security

import org.cqfn.save.gateway.config.ConfigurationProperties
import org.cqfn.save.gateway.utils.StoringServerAuthenticationSuccessHandler

import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

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
) {
    @Bean
    @Order(1)
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.securityMatcher(
        // access to actuator is managed separately
        AndServerWebExchangeMatcher(
            ServerWebExchangeMatchers.pathMatchers("/**"),
            NegatedServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/actuator", "/actuator/**")
            )
        )
    ).authorizeExchange { authorizeExchangeSpec ->
        // this is default data that is required by FE to operate properly
        authorizeExchangeSpec.pathMatchers(
            "/",
            "/login", "/logout",
            "/sec/oauth-providers", "/sec/user",
            "/error",
        )
            .permitAll()
            // all requests to backend are permitted on gateway, if user agent is authenticated in gateway or doesn't have
            // any authentication data at all.
            // backend returns 401 for those endpoints that require authentication
            .pathMatchers("/api/**")
            .access { authentication, authorizationContext ->
                AuthenticatedReactiveAuthorizationManager.authenticated<AuthorizationContext>().check(
                    authentication, authorizationContext
                ).map {
                    if (!it.isGranted) {
                        // if request is not authorized by configured authorization manager, then we allow only requests w/o Authorization hedaer
                        // then backend will return 401, if endpoint is protected for anonymous access
                        AuthorizationDecision(
                            authorizationContext.exchange.request.headers[HttpHeaders.AUTHORIZATION].isNullOrEmpty()
                        )
                    } else {
                        it
                    }
                }
            }
            // resources for frontend
            .pathMatchers("/*.html", "/*.js*", "/*.css", "/img/**", "favicon.ico")
            .permitAll()
    }
        .run {
            authorizeExchange()
                .pathMatchers("/**")
                .authenticated()
        }
        .and().run {
            // FixMe: Properly support CSRF protection https://github.com/analysis-dev/save-cloud/issues/34
            csrf().disable()
        }
        .exceptionHandling {
            it.authenticationEntryPoint(
                HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
            )
        }
        .oauth2Login {
            it.authenticationSuccessHandler(
                DelegatingServerAuthenticationSuccessHandler(
                    StoringServerAuthenticationSuccessHandler(configurationProperties),
                    RedirectServerAuthenticationSuccessHandler("/#/projects"),
                )
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
            .authenticated()
    }
        .and().httpBasic().run {
            val userFromProps = configurationProperties.basicCredentials?.split(' ')?.run {
                User(first(), last(), emptyList())
            }
            if (userFromProps != null) {
                authenticationManager(
                    UserDetailsRepositoryReactiveAuthenticationManager(
                        MapReactiveUserDetailsService(userFromProps)
                    )
                )
            } else {
                this
            }
        }
        .and().build()
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
