package com.saveourtool.save.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SdkTest {
    @Test
    fun `should parse SDK correctly from strings`() {
        listOf(
            "Java:9",
            "openjdk:9"
        ).forEach {
            Assertions.assertEquals(Jdk("9"), it.toSdk())
        }
        listOf(
            "Python:3.5",
            "python:3.5"
        ).forEach {
            Assertions.assertEquals(Python("3.5"), it.toSdk())
        }
    }
}
