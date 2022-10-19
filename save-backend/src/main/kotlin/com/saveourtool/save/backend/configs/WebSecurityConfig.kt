/**
 * Configuration beans for security in different profiles
 */

package com.saveourtool.save.backend.configs

import com.saveourtool.save.authservice.config.NoopWebSecurityConfig
import com.saveourtool.save.authservice.config.WebSecurityConfig
import com.saveourtool.save.authservice.repository.AuthenticationUserRepository
import com.saveourtool.save.authservice.security.ConvertingAuthenticationManager
import com.saveourtool.save.authservice.security.CustomAuthenticationBasicConverter
import com.saveourtool.save.authservice.service.AuthenticationUserDetailsService
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService

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
    UserDetailsService::class,
    UserRepository::class,
)
class BackendWebSecurityConfig

@EnableWebFluxSecurity
@Profile("!secure")
@Import(NoopWebSecurityConfig::class)
class BackendNoopWebSecurityConfig