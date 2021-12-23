/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.gateway.security

import org.cqfn.save.gateway.config.ConfigurationProperties
import org.cqfn.save.gateway.utils.StoringServerAuthenticationSuccessHandler

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler
import reactor.core.publisher.Mono

@EnableWebFluxSecurity
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig(
    private val configurationProperties: ConfigurationProperties,
) {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        authorizeExchange()
            // this is default data that is required by FE to operate properly
            .pathMatchers("/", "/login", "/logout", "/sec/oauth-providers")
            .permitAll()
            // all requests to backend are permitted on gateway, if user agent is authenticated in gateway or doesn't have
            // any authentication data at all.
            // backend returns 401 for those endpoints that require authentication
            .pathMatchers("/api/**")
            .access { authentication, authorizationContext ->
                authentication.map {
                    AuthorizationDecision(
                        it.isAuthenticated ||
                            authorizationContext.exchange.request.headers[HttpHeaders.AUTHORIZATION].also {
                                println("Authorization: $it")
                            }.isNullOrEmpty()
                    )
                }
            }
            // resources for frontend
            .pathMatchers("/*.html", "/*.js*", "img/**", "*.gif", "*.svg")
            .permitAll()
    }
        .and().run {
            authorizeExchange()
                .pathMatchers("/**")
                .authenticated()
        }
        .and().run {
            // FixMe: Properly support CSRF protection https://github.com/diktat-static-analysis/save-cloud/issues/34
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
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
