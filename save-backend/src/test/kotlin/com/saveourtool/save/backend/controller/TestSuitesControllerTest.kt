package com.saveourtool.save.backend.controller

import com.saveourtool.save.backend.SaveApplication
import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.GitRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.TestSuiteRepository
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.scheduling.JobsConfiguration
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.v1
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.*
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.util.*

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
@Import(QuartzAutoConfiguration::class, JobsConfiguration::class)
class TestSuitesControllerTest {
    @Autowired
    lateinit var webClient: WebTestClient

    @Autowired
    lateinit var testSuiteRepository: TestSuiteRepository

    @Autowired
    lateinit var projectRepository: ProjectRepository

    @Autowired
    lateinit var testSuitesSourceRepository: TestSuitesSourceRepository

    @Autowired
    lateinit var gitRepository: GitRepository

    @MockBean
    lateinit var scheduler: Scheduler

    @Test
    fun `should accept test suites and return saved test suites`() {
        val testSuitesSource = testSuitesSourceRepository.getReferenceById(1)
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1",
        )

        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>()
                .consumeWith {
                    val body = it.responseBody!!
                    assertEquals(listOf(testSuite).size, body.size)
                    assertEquals(testSuite.name, body[0].name)
                    assertEquals(testSuite.source, body[0].source)
                    assertEquals(testSuite.version, body[0].version)
                }
        }
    }

    @Test
    fun `saved test suites should be persisted in the DB`() {
        val testSuitesSource = testSuitesSourceRepository.getReferenceById(1)
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1"
        )

        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>()
        }

        val databaseData = testSuiteRepository.findAll()
        assertTrue(databaseData.any { it.source.name == testSuite.source.name && it.name == testSuite.name })
    }

    @Test
    fun `should save only new test suites`() {
        val testSuitesSource = testSuitesSourceRepository.getReferenceById(1)
        val testSuite = TestSuiteDto(
            "test",
            null,
            testSuitesSource.toDto(),
            "1",
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }

        val testSuite2 = TestSuiteDto(
            "test2",
            null,
            testSuitesSource.toDto(),
            "1",
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

    @Suppress("TOO_LONG_FUNCTION")
    @Test
    @WithMockUser
    fun testAllStandardTestSuites() {
        // FIXME: a hardcoded value of url for standard test suites
        val standardTestSuitesSourceUrl = "https://github.com/saveourtool/save-cli"
        val organization = projectRepository.getReferenceById(1).organization
        val git = gitRepository.save(
            Git(
                standardTestSuitesSourceUrl,
                null,
                null,
                organization,
            )
        )
        val testSuitesSource = testSuitesSourceRepository.save(
            TestSuitesSource(
                organization,
                "standard test suites source",
                null,
                git,
                "master",
                "",
            )
        )
        val testSuite = TestSuiteDto(
            "tester",
            null,
            testSuitesSource.toDto(),
            "1",
        )
        saveTestSuites(listOf(testSuite)) {
            expectBody<List<TestSuite>>().consumeWith {
                assertEquals(1, it.responseBody!!.size)
            }
        }
        val allStandardTestSuite = testSuiteRepository.findAll().count { it.source.git.url == standardTestSuitesSourceUrl }
        webClient.get()
            .uri("/api/$v1/allStandardTestSuites")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<List<TestSuiteDto>>()
            .consumeWith {
                requireNotNull(it.responseBody)
                assertEquals(it.responseBody!!.size, allStandardTestSuite)
            }
    }

    @Suppress("TOO_LONG_FUNCTION")
    @Test
    fun testTestSuitesWithSpecificName() {
        // FIXME: a hardcoded value of url for standard test suites
        val standardTestSuitesSourceUrl = "https://github.com/saveourtool/save-cli"
        val organization = projectRepository.getReferenceById(1).organization
        val git = gitRepository.save(
            Git(
                standardTestSuitesSourceUrl,
                null,
                null,
                organization,
            )
        )
        val testSuitesSource = testSuitesSourceRepository.save(
            TestSuitesSource(
                organization,
                "standard test suites source",
                null,
                git,
                "master",
                "",
            )
        )
        val name = "tester"
        val testSuite = TestSuiteDto(
            name,
            null,
            testSuitesSource.toDto(),
            "1"
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
            .uri("/api/$v1/updateStandardTestSuites")
            .exchange()
            .expectStatus()
            .isOk

        verify(scheduler, times(1)).triggerJob(any())
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
