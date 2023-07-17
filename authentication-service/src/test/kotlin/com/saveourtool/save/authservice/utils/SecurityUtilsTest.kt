/**
 * Authentication utilities
 */

package com.saveourtool.save.authservice.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class SecurityUtilsTest {

    private val user = IdAwareUserDetails(
        "name",
        "password",
        "VIEWER",
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

    private fun updateSecurityContext() = SecurityContextHolder.getContext().apply {
        authentication = UsernamePasswordAuthenticationToken(user, null)
        (authentication as UsernamePasswordAuthenticationToken).apply {
            details = AuthenticationDetails(id = user.id)
        }
    }.authentication
}
