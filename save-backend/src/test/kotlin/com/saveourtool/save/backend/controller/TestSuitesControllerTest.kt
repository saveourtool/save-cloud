package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer

import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
@Import(QuartzAutoConfiguration::class)
class TestSuitesControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Autowired
    lateinit var testSuitesSourceRepository: TestSuitesSourceRepository

    @Autowired
    lateinit var testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage

    @MockBean
    lateinit var scheduler: Scheduler

    @Test
    fun `should accept test suites and return saved test suites`() {
        val testSuitesSource = testSuitesSourceRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1",
        )

        saveTestSuites(testSuite) {
            expectBody<TestSuite>()
                .consumeWith {
                    val body = it.responseBody!!
                    assertEquals(testSuite.name, body.name)
                    assertEquals(testSuite.source.name, body.source.name)
                    assertEquals(testSuite.source.organizationName, body.source.organization.name)
                    assertTrue(testSuite.source.latestFetchedVersion != body.source.latestFetchedVersion)
                    assertEquals(testSuite.version, body.source.latestFetchedVersion)
                    assertEquals(testSuite.version, body.version)
                }
        }
    }

    @Test
    fun `saved test suites should be persisted in the DB`() {
        val testSuitesSource = testSuitesSourceRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1"
        )

        saveTestSuites(testSuite) {
            expectBody<TestSuite>()
        }

        val databaseData = testSuiteRepository.findAll()
        assertTrue(databaseData.any { it.source.name == testSuite.source.name && it.name == testSuite.name })
    }

    @Test
    fun `should save only new test suites`() {
        val testSuitesSource = testSuitesSourceRepository.findById(1).get()
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1",
        )
        var testSuiteId: Long? = null
        saveTestSuites(testSuite) {
            expectBody<TestSuite>().consumeWith {
                assertNotNull(it.responseBody)
                testSuiteId = it.responseBody?.requiredId()
            }
        }

        val testSuite2 = TestSuiteDto(
            "test2",
            null,
            testSuitesSource.toDto(),
            "1",
        )
        saveTestSuites(testSuite2) {
            expectBody<TestSuite>().consumeWith {
                assertNotNull(it.responseBody)
                assertTrue(it.responseBody?.requiredId() != testSuiteId)
            }
        }
        saveTestSuites(testSuite) {
            expectBody<TestSuite>().consumeWith {
                assertNotNull(it.responseBody)
                assertEquals(testSuiteId, it.responseBody?.requiredId())
            }
        }
    }

    private fun saveTestSuite(testSuite: TestSuiteDto, spec: WebTestClient.ResponseSpec.() -> Unit) {
        webClient.post()
            .uri("/internal/test-suites/save")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(testSuite))
            .exchange()
            .spec()
    }

    companion object {
        @JvmStatic lateinit var mockServerPreprocessor: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerPreprocessor.checkQueues()
            mockServerPreprocessor.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerPreprocessor.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerPreprocessor = createMockWebServer()
            mockServerPreprocessor.start()
            registry.add("backend.preprocessorUrl") { "http://localhost:${mockServerPreprocessor.port}" }
        }
    }
}
