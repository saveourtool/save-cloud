package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.LocalDateTime
import java.time.Month

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
class TestSuitesControllerTest : DatabaseTestBase() {
    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Test
    fun testConnection() {
        val testSuite = TestSuite(
            TestSuiteType.PROJECT,
            "test",
            0,
            testLocalDateTime
        )

        webClient.post()
            .uri("/saveTestSuite")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuite))
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun checkDataSave() {
        val testSuite = TestSuite(
            TestSuiteType.PROJECT,
            "test",
            0,
            testLocalDateTime
        )

        webClient.post()
            .uri("/saveTestSuite")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuite))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testSuiteRepository.findAll()

        assertTrue(databaseData.any { it.id == testSuite.id && it.projectId == testSuite.projectId })
    }
}
