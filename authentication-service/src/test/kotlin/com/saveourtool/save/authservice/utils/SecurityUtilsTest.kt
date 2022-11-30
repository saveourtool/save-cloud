/**
 * Authentication utilities
 */

package com.saveourtool.save.authservice.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class SecurityUtilsTest {

    private val user = IdentitySourceAwareUserDetails(
        "basic:name",
        "password",
        "VIEWER",
        "basic",
        42
    )

    @Test
    fun `should extract user name`() {
        updateSecurityContext()
        val mockedAuthentication = SecurityContextHolder.getContext().authentication
        Assertions.assertEquals(mockedAuthentication.username(), user.username)
    }

    @Test
    fun `should extract user id`() {
        updateSecurityContext()
        val mockedAuthentication = SecurityContextHolder.getContext().authentication
        Assertions.assertEquals(mockedAuthentication.userId(), user.id)
    }

    @Test
    fun `should extract user identity source`() {
        updateSecurityContext()
        val mockedAuthentication = SecurityContextHolder.getContext().authentication
        Assertions.assertEquals(mockedAuthentication.identitySource(), user.identitySource)
    }

    private fun updateSecurityContext() = SecurityContextHolder.getContext().apply {
        authentication = UsernamePasswordAuthenticationToken(user, null)
        (authentication as UsernamePasswordAuthenticationToken).apply {
            details = AuthenticationDetails(id = user.id, user.identitySource)
        }
    }.authentication
}
