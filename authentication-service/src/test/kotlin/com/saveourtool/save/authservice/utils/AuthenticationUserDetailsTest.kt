package com.saveourtool.save.authservice.utils

import com.saveourtool.save.authservice.utils.AuthenticationUserDetails.Companion.toAuthenticationUserDetails
import com.saveourtool.save.utils.AUTHORIZATION_ID
import com.saveourtool.save.utils.AUTHORIZATION_NAME
import com.saveourtool.save.utils.AUTHORIZATION_ROLES
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.server.ServerWebExchange

class AuthenticationUserDetailsTest {
    @Test
    fun toAuthenticationUserDetailsValid() {
        val (serverWebExchange, httpHeaders) = mockServerWebExchange()
        httpHeaders[AUTHORIZATION_ID] = "123"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        val result = serverWebExchange.toAuthenticationUserDetails()

        Assertions.assertNotNull(result)
        Assertions.assertEquals(123, result?.id)
        Assertions.assertEquals("name", result?.name)
        Assertions.assertEquals("ROLE", result?.role)
    }

    @Test
    fun toAuthenticationUserDetailsDuplicate() {
        val (serverWebExchange, httpHeaders) = mockServerWebExchange()
        httpHeaders[AUTHORIZATION_ID] = listOf("123", "321")
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(serverWebExchange.toAuthenticationUserDetails())
    }

    @Test
    fun toAuthenticationUserDetailsMissed() {
        val (serverWebExchange, httpHeaders) = mockServerWebExchange()
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(serverWebExchange.toAuthenticationUserDetails())
    }

    @Test
    fun toAuthenticationUserDetailsInvalid() {
        val (serverWebExchange, httpHeaders) = mockServerWebExchange()
        httpHeaders[AUTHORIZATION_ID] = "not_integer"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        assertThrows<NumberFormatException> {
            serverWebExchange.toAuthenticationUserDetails()
        }
    }

    private fun mockServerWebExchange(): Pair<ServerWebExchange, HttpHeaders> {
        val serverWebExchange = mock<ServerWebExchange>()
        val serverHttpRequest = mock<ServerHttpRequest>()
        val httpHeaders = HttpHeaders()
        whenever(serverWebExchange.request).thenReturn(serverHttpRequest)
        whenever(serverHttpRequest.headers).thenReturn(httpHeaders)
        return serverWebExchange to httpHeaders
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
