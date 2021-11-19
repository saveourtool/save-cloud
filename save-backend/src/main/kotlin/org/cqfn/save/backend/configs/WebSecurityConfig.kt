package org.cqfn.save.backend.configs

import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
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
            .and().formLogin()
            .and().build()
    }
}
