/**
 * Configuration beans for security in different profiles
 */

package org.cqfn.save.backend.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@Profile("!test")
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class WebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.run {
        // `CollectionView` is a public page
        authorizeExchange()
            .pathMatchers("/", "/projects/not-deleted")
            .permitAll()
    }
        .and().run {
            authorizeExchange()
                .pathMatchers("/**")
                .authenticated()
        }
        .and().run {
            // FixMe: Properly support CSRF protection
            csrf().disable()
        }
        .formLogin()
        .and().build()
}

@EnableWebFluxSecurity
@Profile("test")
@Suppress("MISSING_KDOC_TOP_LEVEL", "MISSING_KDOC_CLASS_ELEMENTS", "MISSING_KDOC_ON_FUNCTION")
class NoopWebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = http.authorizeExchange()
        .anyExchange()
        .permitAll()
        .and()
        .csrf()
        .disable()
        .build()
}
