package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.controllers.ProjectController
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto
import org.junit.jupiter.api.Assertions.assertEquals

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(ProjectController::class),
)
class TestInitializeControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun testConnection() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            "WarnPlugin",
            testSuite.id!!,
            "newHash",
            listOf("tag"),
        )

        webClient.post()
            .uri("/internal/initializeTests?executionId=2")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        assertNotNull(testRepository.findAllByTestSuiteId(2))
        assertNotNull(testRepository.findByHashAndFilePathAndTestSuiteId("newHash", "testPath", 2))
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun checkDataSave() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testPath",
            "WarnPlugin",
            testSuite.id!!,
            "newHash2",
            listOf("tag"),
        )
        webClient.post()
            .uri("/internal/initializeTests?executionId=2")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testRepository.findAll()

        assertTrue(databaseData.any { it.testSuite.id == test.testSuiteId && it.filePath == test.filePath && it.hash == test.hash })
    }

    @Test
    @Suppress("UnsafeCallOnNullableType")
    fun checkInitializeWithoutExecution() {
        val testSuite = testSuiteRepository.findById(2).get()
        val test = TestDto(
            "testWithoutExecution",
            "WarnPlugin",
            testSuite.id!!,
            "newHash123",
            listOf("tag"),
        )
        webClient.post()
            .uri("/internal/initializeTests")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(listOf(test)))
            .exchange()
            .expectStatus()
            .isOk

        val databaseData = testRepository.findAll()

        assertTrue(databaseData.any { it.testSuite.id == test.testSuiteId && it.filePath == test.filePath && it.hash == test.hash })
    }

    @Test
    fun `should return test executions in batches`() {
        webClient.get()
            .uri("/internal/getTestBatches?agentId=container-1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestBatch>()
            .consumeWith { entityExchangeResult ->
                val batch = entityExchangeResult.responseBody!!
                println(batch)
                assertTrue(batch.tests.isNotEmpty())
                assertEquals(10, batch.tests.size)
            }

        webClient.get()
            .uri("/internal/getTestBatches?agentId=container-1")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<TestBatch>()
            .consumeWith {
                assertTrue(it.responseBody!!.tests.size == 3) { "Expected 3 tests, but got ${it.responseBody!!.tests} instead" }
            }
    }
}
