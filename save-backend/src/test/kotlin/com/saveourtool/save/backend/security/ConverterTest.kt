package com.saveourtool.save.backend.security

import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.CustomAuthenticationBasicConverter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import java.util.Base64

class ConverterTest {
    private val customAuthenticationBasicConverter = CustomAuthenticationBasicConverter()

    @Test
    fun `should convert`() {
        val authentication = customAuthenticationBasicConverter.convert(
            MockServerWebExchange.from(
                MockServerHttpRequest.get("any")
                    .header(HttpHeaders.AUTHORIZATION, "Basic ${"user:".base64Encode()}")
                    .header("X-Authorization-Source", "basic")
            )
        )
            .block()!!

        Assertions.assertInstanceOf(UsernamePasswordAuthenticationToken::class.java, authentication)
        Assertions.assertInstanceOf(AuthenticationDetails::class.java, authentication.details)
        Assertions.assertEquals("basic:user", authentication.principal)
        Assertions.assertEquals("basic", (authentication.details as AuthenticationDetails).identitySource)
    }
}

private fun String.base64Encode() = Base64.getEncoder().encodeToString(toByteArray())
