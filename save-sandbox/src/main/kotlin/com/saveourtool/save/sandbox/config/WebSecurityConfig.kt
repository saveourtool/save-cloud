/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.sandbox.config

import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.authservice.config.WebSecurityConfig
import com.saveourtool.save.authservice.repository.UserRepository
import com.saveourtool.save.authservice.security.ConvertingAuthenticationManager
import com.saveourtool.save.authservice.security.CustomAuthenticationBasicConverter
import com.saveourtool.save.authservice.service.UserDetailsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("secure")
@ComponentScan("com.saveourtool.save.authservice.config.WebSecurityConfig")
@Import(WebSecurityConfig::class,
    ConvertingAuthenticationManager::class,
    CustomAuthenticationBasicConverter::class,
    UserDetailsService::class,
    UserRepository::class,
)
class SandboxWebSecurityConfig(
    @Autowired private val webSecurityConfig: WebSecurityConfig,
    @Autowired private val convertingAuthenticationManager: ConvertingAuthenticationManager,
    @Autowired private val customAuthenticationBasicConverter: CustomAuthenticationBasicConverter,
) {
    init {
        println("\n\n\nSandboxWebSecurityConfig")
    }
}

@EnableWebFluxSecurity
@Profile("!secure")
@ComponentScan("com.saveourtool.save.authservice.config.NoopWebSecurityConfig")
@Import(SandboxNoopWebSecurityConfig::class)
class SandboxNoopWebSecurityConfig(
    @Autowired private val noopWebSecurityConfig: NoopWebSecurityConfig
) {
    init {
        println("\n\n\nSandboxNoopWebSecurityConfig")
    }
}

