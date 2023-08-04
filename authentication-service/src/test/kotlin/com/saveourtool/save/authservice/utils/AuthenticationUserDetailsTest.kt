package com.saveourtool.save.authservice.utils

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails.Companion.toAuthenticationUserDetails
import com.saveourtool.save.utils.AUTHORIZATION_ID
import com.saveourtool.save.utils.AUTHORIZATION_NAME
import com.saveourtool.save.utils.AUTHORIZATION_ROLES
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders

class AuthenticationUserDetailsTest {
    @Test
    fun toAuthenticationUserDetailsValid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "123"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        val result = httpHeaders.toAuthenticationUserDetails()

        Assertions.assertNotNull(result)
        Assertions.assertEquals(123, result?.id)
        Assertions.assertEquals("name", result?.name)
        Assertions.assertEquals("ROLE", result?.role)
    }

    @Test
    fun toAuthenticationUserDetailsDuplicate() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = listOf("123", "321")
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(httpHeaders.toAuthenticationUserDetails())
    }

    @Test
    fun toAuthenticationUserDetailsMissed() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(httpHeaders.toAuthenticationUserDetails())
    }

    @Test
    fun toAuthenticationUserDetailsInvalid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "not_integer"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        assertThrows<NumberFormatException> {
            httpHeaders.toAuthenticationUserDetails()
        }
    }

    @Test
    fun populateHeaders() {
        val authenticationUserDetails = AuthenticationUserDetails(
            id = 123,
            name = "name",
            role = "ROLE"
        )
        val httpHeaders = HttpHeaders()
        authenticationUserDetails.populateHeaders(httpHeaders)

        Assertions.assertEquals(listOf("123"), httpHeaders[AUTHORIZATION_ID])
        Assertions.assertEquals(listOf("name"), httpHeaders[AUTHORIZATION_NAME])
        Assertions.assertEquals(listOf("ROLE"), httpHeaders[AUTHORIZATION_ROLES])
    }
}
