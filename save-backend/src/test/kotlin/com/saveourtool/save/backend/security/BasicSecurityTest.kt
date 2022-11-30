package com.saveourtool.save.backend.security

import com.saveourtool.save.authservice.repository.AuthenticationUserRepository
import com.saveourtool.save.authservice.security.ConvertingAuthenticationManager
import com.saveourtool.save.authservice.service.AuthenticationUserDetailsService
import com.saveourtool.save.backend.repository.OriginalLoginRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.service.UserDetailsService
import com.saveourtool.save.authservice.utils.AuthenticationDetails
import com.saveourtool.save.entities.User
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(
    UserDetailsService::class,
    ConvertingAuthenticationManager::class,
    AuthenticationUserDetailsService::class,
    AuthenticationUserRepository::class,
)
@ActiveProfiles("secure")
class BasicSecurityTest {
    @Autowired
    private lateinit var convertingAuthenticationManager: ConvertingAuthenticationManager
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var authenticationUserRepository: AuthenticationUserRepository
    @MockBean private lateinit var originalLoginRepository: OriginalLoginRepository
    @MockBean private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @BeforeEach
    fun setUp() {
        whenever(authenticationUserRepository.findByName("user")).thenReturn(
            User("user", null, "ROLE_USER", "basic").apply {
                id = 99
            }
        )
    }

    @Test
    fun `should allow access for registered user`() {
        val authentication = tryAuthenticate("basic:user", "basic")

        Assertions.assertTrue(authentication.isAuthenticated)
    }

    @Test
    fun `should forbid requests if user has the same name but different source`() {
        Assertions.assertThrows(BadCredentialsException::class.java) {
            tryAuthenticate("github:user", "github")
        }
    }

    @Test
    fun `should forbid requests if user has the same name but no source`() {
        Assertions.assertThrows(BadCredentialsException::class.java) {
            tryAuthenticate(":user", "")
        }
    }

    private fun tryAuthenticate(principal: String, identitySource: String) = convertingAuthenticationManager.authenticate(
        UsernamePasswordAuthenticationToken(
            principal,
            ""
        ).apply {
            details = AuthenticationDetails(id = 99, identitySource = identitySource)
        }
    )
        .block()!!
}
