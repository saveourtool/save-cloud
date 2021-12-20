/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.gateway.security

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.authentication.logout.HttpStatusReturningServerLogoutSuccessHandler

@EnableWebFluxSecurity
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        // `CollectionView` is a public page
        // todo: backend should tell which endpoint is public, and gateway should provide user data
        authorizeExchange()
            .pathMatchers("/", "/login", "/info/**", "/api/projects/not-deleted", "/save-frontend*.js*")
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
                RedirectServerAuthenticationSuccessHandler("/#/projects")
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
