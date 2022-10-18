package com.saveourtool.save.sandbox.security

import com.saveourtool.save.sandbox.service.SandboxUserDetailsService
import com.saveourtool.save.sandbox.utils.extractUserNameAndIdentitySource
import com.saveourtool.save.utils.AuthenticationDetails
import com.saveourtool.save.utils.IdentitySourceAwareUserDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.cast
import reactor.kotlin.core.publisher.switchIfEmpty

@Configuration
@ComponentScan("com.saveourtool.save.authservice.security.ConvertingAuthenticationManager")
class SandboxConvertingAuthenticationManager
