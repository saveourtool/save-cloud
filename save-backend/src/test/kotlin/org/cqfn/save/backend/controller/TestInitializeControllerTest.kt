package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.utils.DatabaseTestBase
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
class TestInitializeControllerTest : DatabaseTestBase() {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testInitRepository: TestRepository

    private val testLocalDateTime = LocalDateTime.of(2020, Month.APRIL, 10, 16, 30, 20)

    @Test
    fun testConnection() {
        val test = org.cqfn.save.entities.Test(
                "expectedPath",
                "testPath",
                testLocalDateTime,
                0,
                "HASH"
        )

        webClient.post()
                .uri("/initializeTests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(listOf(test)))
                .exchange()
                .expectStatus()
                .isOk
    }

    @Test
    fun checkDataSave() {
        val test = org.cqfn.save.entities.Test(
                "expectedPath",
                "testPath",
                testLocalDateTime,
                0,
                "HASHANOTHER"
        )
        webClient.post()
                .uri("/initializeTests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(listOf(test)))
                .exchange()
                .expectStatus()
                .isOk

        val databaseData = testInitRepository.findAll()

        assert(databaseData.any { it.id == test.id && it.expectedFilePath == test.expectedFilePath })
    }
}