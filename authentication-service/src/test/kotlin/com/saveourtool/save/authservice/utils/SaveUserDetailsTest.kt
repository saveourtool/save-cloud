package com.saveourtool.save.authservice.utils

import com.saveourtool.save.authservice.utils.SaveUserDetails.Companion.toSaveUserDetails
import com.saveourtool.save.utils.AUTHORIZATION_ID
import com.saveourtool.save.utils.AUTHORIZATION_NAME
import com.saveourtool.save.utils.AUTHORIZATION_ROLES
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders

class SaveUserDetailsTest {
    @Test
    fun toSaveUserDetailsValid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "123"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"
        val result = httpHeaders.toSaveUserDetails()

        Assertions.assertNotNull(result)
        Assertions.assertEquals(123, result?.id)
        Assertions.assertEquals("name", result?.name)
        Assertions.assertEquals("ROLE", result?.role)
    }

    @Test
    fun toSaveUserDetailsDuplicate() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = listOf("123", "321")
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(httpHeaders.toSaveUserDetails())
    }

    @Test
    fun toSaveUserDetailsMissed() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        Assertions.assertNull(httpHeaders.toSaveUserDetails())
    }

    @Test
    fun toSaveUserDetailsInvalid() {
        val httpHeaders = HttpHeaders()
        httpHeaders[AUTHORIZATION_ID] = "not_integer"
        httpHeaders[AUTHORIZATION_NAME] = "name"
        httpHeaders[AUTHORIZATION_ROLES] = "ROLE"

        assertThrows<NumberFormatException> {
            httpHeaders.toSaveUserDetails()
        }
    }

    @Test
    fun populateHeaders() {
        val SaveUserDetails = SaveUserDetails(
            id = 123,
            name = "name",
            role = "ROLE",
            token = "N/A",
        )
        val httpHeaders = HttpHeaders()
        SaveUserDetails.populateHeaders(httpHeaders)

        Assertions.assertEquals(listOf("123"), httpHeaders[AUTHORIZATION_ID])
        Assertions.assertEquals(listOf("name"), httpHeaders[AUTHORIZATION_NAME])
        Assertions.assertEquals(listOf("ROLE"), httpHeaders[AUTHORIZATION_ROLES])
        Assertions.assertEquals(
            setOf(AUTHORIZATION_ID, AUTHORIZATION_NAME, AUTHORIZATION_ROLES),
            httpHeaders.keys,
        )
    }
}
