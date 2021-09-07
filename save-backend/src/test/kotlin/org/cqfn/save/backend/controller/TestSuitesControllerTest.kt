package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.junit.Assert
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.net.HttpURLConnection
import java.time.Instant
import java.util.Date

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
)
class TestSuitesControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @MockBean
    lateinit var scheduler: Scheduler

    @Test
    fun `should accept test suites and return saved test suites`() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            project,
            "save.properties"
        )

        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>()
                .consumeWith {
                    val body = it.responseBody!!
                    Assert.assertEquals(listOf(testSuite).size, body.size)
                    Assert.assertEquals(testSuite.name, body[0].name)
                    Assert.assertEquals(testSuite.project, body[0].project)
                    Assert.assertEquals(testSuite.type, body[0].type)
                }
        }
    }

    @Test
    fun `saved test suites should be persisted in the DB`() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            project,
            "save.properties"
        )

        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>()
        }

        val databaseData = testSuiteRepository.findAll()
        assertTrue(databaseData.any { it.project?.id == testSuite.project?.id && it.name == testSuite.name })
    }

    @Test
    fun `should save only new test suites`() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            project,
            "save.properties"
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }

        val testSuite2 = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test2",
            project,
            "save.properties"
        )
        saveTestSuites(listOf(testSuite, testSuite2)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(2, it.responseBody!!.size)
            }
        }
    }

    private fun saveTestSuites(testSuites: List<TestSuiteDto>, spec: WebTestClient.ResponseSpec.() -> Unit) {
        webClient.post()
            .uri("/saveTestSuites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuites))
            .exchange()
            .spec()
    }

    @Test
    fun testAllStandardTestSuites() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.STANDARD,
            "tester",
            project,
            "save.properties"
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }
        val allStandardTestSuite = testSuiteRepository.findAll().count { it.type == TestSuiteType.STANDARD }
        webClient.get()
            .uri("/allStandardTestSuites")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<TestSuiteDto>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(it.responseBody!!.size, allStandardTestSuite)
            }
    }

    @Test
    fun testTestSuitesWithSpecificName() {
        val project = projectRepository.findById(1).get()
        val name = "tester"
        val testSuite = TestSuiteDto(
            TestSuiteType.STANDARD,
            name,
            project,
            "save.properties"
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }

        webClient.get()
            .uri("/testSuitesWithName?name=$name")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<TestSuiteDto>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                // FixMe could be moved into assert block after https://github.com/cqfn/diKTat/issues/1047
                val responseBody = it.responseBody!!
                assertEquals(responseBody[0].name, name)
            }
    }

    @Test
    fun testUpdateStandardTestSuites() {
        whenever(scheduler.scheduleJob(any())).thenReturn(Date.from(Instant.now()))

        webClient.post()
            .uri("/updateStandardTestSuites")
            .exchange()
            .expectStatus()
            .isOk

        verify(scheduler, times(1)).triggerJob(any())
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = MockWebServer()
            (mockServerPreprocessor.dispatcher as QueueDispatcher).setFailFast(
                MockResponse().setResponseCode(HttpURLConnection.HTTP_NOT_FOUND)
            )
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
