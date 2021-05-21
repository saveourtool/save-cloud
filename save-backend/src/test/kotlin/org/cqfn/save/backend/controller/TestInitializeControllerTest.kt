package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.time.LocalDateTime
import java.time.Month

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class TestInitializeControllerTest {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testInitRepository: TestRepository

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Test
    fun testConnection() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            testSuite.id!!,
            "newHash",
        )

        webClient.post()
            .uri("/initializeTests?executionId=1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        assertNotNull(testInitRepository.findByHash("newHash") != null)
    }

    @Test
    fun checkDataSave() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            testSuite.id!!,
            "newHash2",

        )
        webClient.post()
            .uri("/initializeTests?executionId=1")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testInitRepository.findAll()

        assertTrue(databaseData.any { it.testSuite.id == test.testSuiteId && it.filePath == test.filePath && it.hash == test.hash })
    }

    @Test
    fun checkServiceData() {
        webClient.get()
            .uri("/getTestBatches?agentId=container-1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestBatch>()
            .consumeWith {
                println(it.responseBody)
                assertTrue(it.responseBody!!.tests.isNotEmpty() && it.responseBody!!.tests.size == 20)
            }

        webClient.get()
            .uri("/getTestBatches?agentId=container-1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestBatch>()
            .consumeWith {
                assertTrue(it.responseBody!!.tests.size == 3) { "Expected 3 tests, but got ${it.responseBody.tests} instead" }
            }
    }

    @Test
    fun checkDifferentExecutions() {
        webClient.get()
            .uri("/getTestBatches?agentId=container-3")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestBatch>()
            .consumeWith {
                println(it.responseBody)
                assertTrue(it.responseBody!!.tests.isNotEmpty())
            }
    }
}
