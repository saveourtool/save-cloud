/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.gateway.security

import org.cqfn.save.gateway.config.ConfigurationProperties
import org.cqfn.save.gateway.utils.StoringServerAuthenticationSuccessHandler

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthenticatedReactiveAuthorizationManager
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.DelegatingServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler
import org.springframework.security.web.server.authorization.AuthorizationContext

@EnableWebFluxSecurity
@Suppress(
    "MISSING_KDOC_TOP_LEVEL",
    "MISSING_KDOC_CLASS_ELEMENTS",
    "MISSING_KDOC_ON_FUNCTION",
    "TOO_LONG_FUNCTION",
)
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
            .pathMatchers("/*.html", "/*.js*", "/img/**")
            .permitAll()
    }
        .and().run {
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
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
