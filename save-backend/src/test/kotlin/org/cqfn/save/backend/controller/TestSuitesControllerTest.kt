package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.controllers.ProjectController
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.backend.scheduling.StandardSuitesUpdateScheduler
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.testsuite.TestSuiteType
import org.cqfn.save.testutils.checkQueues
import org.cqfn.save.testutils.createMockWebServer

import okhttp3.mockwebserver.MockWebServer
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.time.Instant
import java.util.Date

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(StandardSuitesUpdateScheduler::class),
    MockBean(ProjectController::class),
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
            null,
            project,
            "save.properties"
        )

        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>()
                .consumeWith {
                    val body = it.responseBody!!
                    assertEquals(listOf(testSuite).size, body.size)
                    assertEquals(testSuite.name, body[0].name)
                    assertEquals(testSuite.project, body[0].project)
                    assertEquals(testSuite.type, body[0].type)
                }
        }
    }

    @Test
    fun `saved test suites should be persisted in the DB`() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.PROJECT,
            "test",
            null,
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
            null,
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
            null,
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
            .uri("/internal/saveTestSuites")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuites))
            .exchange()
            .spec()
    }

    @Test
    @WithMockUser
    fun testAllStandardTestSuites() {
        val project = projectRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            TestSuiteType.STANDARD,
            "tester",
            null,
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
            .uri("/api/allStandardTestSuites")
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
            null,
            project,
            "save.properties"
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }

        webClient.get()
            .uri("/internal/standardTestSuitesWithName?name=$name")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<TestSuite>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(it.responseBody!!.first().name, name)
            }
    }

    @Test
    @WithMockUser(roles = ["SUPER_ADMIN"])
    fun testUpdateStandardTestSuites() {
        whenever(scheduler.scheduleJob(any())).thenReturn(Date.from(Instant.now()))

        webClient.post()
            .uri("/api/updateStandardTestSuites")
            .exchange()
            .expectStatus()
            .isOk

        verify(scheduler, times(1)).triggerJob(any())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TestSuitesControllerTest::class.java)
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = createMockWebServer(logger)
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
