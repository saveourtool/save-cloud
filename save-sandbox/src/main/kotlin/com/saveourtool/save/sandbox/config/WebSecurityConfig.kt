/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.sandbox.config

import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.authservice.config.WebSecurityConfig
import com.saveourtool.save.authservice.repository.AuthenticationUserRepository
import com.saveourtool.save.authservice.security.ConvertingAuthenticationManager
import com.saveourtool.save.authservice.security.CustomAuthenticationBasicConverter
import com.saveourtool.save.authservice.service.AuthenticationUserDetailsService
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Profile("secure")
@Import(
    WebSecurityConfig::class,
    ConvertingAuthenticationManager::class,
    CustomAuthenticationBasicConverter::class,
    AuthenticationUserDetailsService::class,
    AuthenticationUserRepository::class,
)
class SandboxWebSecurityConfig

@EnableWebFluxSecurity
@Profile("!secure")
@Import(NoopWebSecurityConfig::class)
class SandboxNoopWebSecurityConfig

