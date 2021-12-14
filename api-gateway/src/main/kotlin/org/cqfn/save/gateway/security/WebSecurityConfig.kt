/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.gateway.security

import org.cqfn.save.gateway.config.ConfigurationProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers

@EnableWebFluxSecurity
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig(
    private val configurationProperties: ConfigurationProperties,
) {
    @Bean
    @Order(1)
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http
        .securityMatcher(
            // access to actuator is managed separately
            AndServerWebExchangeMatcher(
                ServerWebExchangeMatchers.pathMatchers("/**"),
                NegatedServerWebExchangeMatcher(
                    ServerWebExchangeMatchers.pathMatchers("/actuator", "/actuator/**")
                )
            )
        )
        .run {
            // `CollectionView` is a public page
            // todo: backend should tell which endpoint is public, and gateway should provide user data
            authorizeExchange()
                .pathMatchers("/", "/info/**", "/api/projects/not-deleted", "/save-frontend*.js*")
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
        .oauth2Login {
            it.authenticationSuccessHandler(RedirectServerAuthenticationSuccessHandler("/#/projects"))
        }
        .build()

    @Bean
    @Order(2)
    fun actuatorSecurityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        authorizeExchange()
            .pathMatchers("/actuator/**")
            .authenticated()
    }
        .and().httpBasic {
            it.authenticationManager(
                UserDetailsRepositoryReactiveAuthenticationManager(
                    MapReactiveUserDetailsService(
                        configurationProperties.basicCredentials.split(' ').run {
                            User(first(), last(), emptyList())
                        }
                    )
                )
            )
        }
        .build()
}

/**
 * @return a bean with default [PasswordEncoder], that can be used throughout the application
 */
@Bean
fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
