package org.cqfn.save.backend.configs

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@Profile("!test")
class WebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain {
        return http.run {
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
}

@EnableWebFluxSecurity
@Profile("test")
class NoopWebSecurityConfig {
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain {
        return http.authorizeExchange()
            .anyExchange()
            .permitAll()
            .and()
            .csrf()
            .disable()
            .build()
    }
}
