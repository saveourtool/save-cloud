package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.test.TestDto
import org.cqfn.save.testsuite.TestSuiteType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
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

    @Test
    fun testConnection() {
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val testSuite = TestSuite(TestSuiteType.PROJECT, "test", project, LocalDateTime.now())
        val test = org.cqfn.save.entities.Test(
            "expectedPath",
            "testPath",
            testLocalDateTime,
            testSuite,
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
        val project = Project("Huawei", "huaweiName", "manual", "huaweiUrl", "description")
        val testSuite = TestSuite(TestSuiteType.PROJECT, "test", project, LocalDateTime.now())
        val test = org.cqfn.save.entities.Test(
            "expectedPath",
            "testPath",
            testLocalDateTime,
            testSuite,
        )
        webClient.post()
            .uri("/initializeTests")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testInitRepository.findAll()

        assertTrue(databaseData.any { it.id == test.id && it.expectedFilePath == test.expectedFilePath })
    }

    @Test
    fun checkServiceData() {
        webClient.get()
            .uri("/getTestBatches")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<List<TestDto>>(List::class.java))
            .value<Nothing> {
                assertTrue(it.isNotEmpty() && it.size == 20)
            }

        webClient.get()
            .uri("/getTestBatches")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(ParameterizedTypeReference.forType<List<TestDto>>(List::class.java))
            .value<Nothing> {
                assertTrue(it.isNotEmpty() && it.size == 1)
            }
    }
}
