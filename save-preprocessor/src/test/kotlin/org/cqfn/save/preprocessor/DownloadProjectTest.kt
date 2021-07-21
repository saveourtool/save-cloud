package org.cqfn.save.preprocessor

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Project
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType
import org.cqfn.save.preprocessor.config.ConfigProperties
import org.cqfn.save.preprocessor.controllers.DownloadProjectController
import org.cqfn.save.preprocessor.service.TestDiscoveringService
import org.cqfn.save.preprocessor.utils.RepositoryVolume
import org.cqfn.save.testsuite.TestSuiteType

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import org.cqfn.save.domain.Sdk
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters

import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

import kotlin.io.path.ExperimentalPathApi

@ExperimentalPathApi
@WebFluxTest(controllers = [DownloadProjectController::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DownloadProjectTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val configProperties: ConfigProperties,
    @Autowired private val objectMapper: ObjectMapper
) : RepositoryVolume {
    private val binFolder = "${configProperties.repository}/binFolder"
    private val binFilePath = "$binFolder/program"
    private val propertyPath = "$binFolder/save.properties"
    @MockBean private lateinit var testDiscoveringService: TestDiscoveringService

    @BeforeEach
    fun webClientSetUp() {
        webClient.mutate().responseTimeout(Duration.ofSeconds(2)).build()
        whenever(testDiscoveringService.getRootTestConfig(any())).thenReturn(mock())
    }

    @BeforeAll
    fun setUp() {
        File(binFolder).mkdir()
        File(propertyPath).createNewFile()
        File(binFilePath).createNewFile()
        File(binFilePath).writeText("echo 0")
    }

    @Test
    fun testBadRequest() {
        val project = Project("owner", "someName", "wrongGit", "descr")
        val wrongRepo = GitDto("wrongGit")
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(), ExecutionStatus.PENDING, "1",
            "foo", 0, 20, ExecutionType.GIT, "0.0.1", 0, 0, 0, Sdk.Default.toString()).apply {
            id = 97L
        }
        val request = ExecutionRequest(project, wrongRepo, sdk = listOf(Sdk.Default), executionId = execution.id)
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
        Thread.sleep(2000)  // Time for request to delete directory
        Assertions.assertFalse(File("${configProperties.repository}/${wrongRepo.url.hashCode()}").exists())
    }

    /**
     * This one covers logic of connecting to services
     */
    @Suppress("TOO_LONG_FUNCTION")
    @Test
    fun testCorrectDownload() {
        val project = Project("owner", "someName", "https://github.com/cqfn/save.git", "descr").apply {
            id = 42L
        }
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(), ExecutionStatus.PENDING, "1",
            "foo", 0, 20, ExecutionType.GIT, "0.0.1", 0, 0, 0, Sdk.Default.toString()).apply {
            id = 99L
        }
        val validRepo = GitDto("https://github.com/cqfn/save.git")
        val request = ExecutionRequest(project, validRepo, "examples/kotlin-diktat/save.properties", listOf(Sdk.Default), execution.id)
        // /createExecution
        mockServerBackend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )
        // /saveTestSuites
        mockServerBackend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(
                    listOf(
                        TestSuite(TestSuiteType.PROJECT, "", project, LocalDateTime.now(), "save.properties")
                    )
                )),
        )
        // /initializeTests?executionId=$executionId
        mockServerBackend.enqueue(
            MockResponse()
                .setResponseCode(200)
        )
        // /initializeAgents
        mockServerOrchestrator.enqueue(
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = CompletableFuture.supplyAsync {
            listOf(
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS),
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS),
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS),
                mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS)
            )
        }
        webClient.post()
            .uri("/upload")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(request))
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo("Clone pending")
        Assertions.assertTrue(File("${configProperties.repository}/${validRepo.url.hashCode()}").exists())
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach { Assertions.assertNotNull(it) }
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    @Test
    fun testSaveProjectAsBinaryFile() {
        val binFile = File(binFilePath)
        val property = File(propertyPath)
        val project = Project("owner", "someName", null, "descr").apply {
            id = 42L
        }
        val execution = Execution(project, LocalDateTime.now(), LocalDateTime.now(), ExecutionStatus.PENDING, "1",
            "foo", 0, 20, ExecutionType.GIT, "0.0.1", 0, 0, 0, Sdk.Default.toString()).apply {
            id = 98L
        }
        val request = ExecutionRequestForStandardSuites(project, emptyList(), listOf(Sdk.Default))
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("executionRequestForStandardSuites", request)
        bodyBuilder.part("property", FileSystemResource(property))
        bodyBuilder.part("binFile", FileSystemResource(binFile))

        mockServerBackend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(execution))
        )
        mockServerBackend.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(
                    listOf(
                        TestSuite(TestSuiteType.PROJECT, "", project, LocalDateTime.now(), "save.properties")
                    )
                )),
        )
        // fixme: preprocessor should initialize tests for execution here
        mockServerOrchestrator.enqueue(
            MockResponse()
                .setResponseCode(200)
        )
        val assertions = CompletableFuture.supplyAsync {
            listOf(
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS),
                mockServerBackend.takeRequest(60, TimeUnit.SECONDS),
                mockServerOrchestrator.takeRequest(60, TimeUnit.SECONDS)
            )
        }

        webClient.post()
            .uri("/uploadBin")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .exchange()
            .expectStatus()
            .isAccepted
            .expectBody<String>()
            .isEqualTo("Clone pending")
        Thread.sleep(2500)

        Assertions.assertTrue(File("${configProperties.repository}/${binFile.name.hashCode()}").exists())
        assertions.orTimeout(60, TimeUnit.SECONDS).join().forEach { Assertions.assertNotNull(it) }
        Assertions.assertEquals("echo 0", File("${configProperties.repository}/${binFile.name.hashCode()}/program").readText())
    }

    @AfterEach
    fun removeTestDir() {
        File(configProperties.repository).deleteRecursively()
    }

    @AfterAll
    fun removeBinDir() {
        File(binFolder).deleteRecursively()
    }

    companion object {
        @JvmStatic
        lateinit var mockServerBackend: MockWebServer

        @JvmStatic
        lateinit var mockServerOrchestrator: MockWebServer

        @AfterAll
        fun tearDown() {
            mockServerBackend.shutdown()
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerBackend = MockWebServer()
            (mockServerBackend.dispatcher as QueueDispatcher).setFailFast(true)
            mockServerBackend.start()
            mockServerOrchestrator = MockWebServer()
            (mockServerOrchestrator.dispatcher as QueueDispatcher).setFailFast(true)
            mockServerOrchestrator.start()
            registry.add("save.backend") { "http://localhost:${mockServerBackend.port}" }
            registry.add("save.orchestrator") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}
