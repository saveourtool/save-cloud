package com.saveourtool.save.backend

import com.saveourtool.save.backend.controllers.ProjectController
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.GitRepository
import com.saveourtool.save.backend.repository.OrganizationRepository
import com.saveourtool.save.backend.repository.ProjectRepository
import com.saveourtool.save.backend.repository.TestSuitesSourceRepository
import com.saveourtool.save.backend.service.TestSuitesSourceService
import com.saveourtool.save.backend.storage.FileStorage
import com.saveourtool.save.backend.storage.TestSuitesSourceSnapshotStorage
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.backend.utils.MySqlExtension
import com.saveourtool.save.backend.utils.mutateMockedUser
import com.saveourtool.save.domain.Jdk
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.toFileInfo
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import com.saveourtool.save.testutils.checkQueues
import com.saveourtool.save.testutils.cleanup
import com.saveourtool.save.testutils.createMockWebServer
import com.saveourtool.save.testutils.enqueue
import com.saveourtool.save.utils.toByteBufferFlux
import com.saveourtool.save.utils.toDataBufferFlux
import com.saveourtool.save.v1

import io.kotest.matchers.collections.shouldExist
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import reactor.core.scheduler.Schedulers
import java.nio.file.Path

import java.time.Instant
import kotlin.io.path.*

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
@MockBeans(
    MockBean(ProjectController::class),
)
class CloneRepoTest {
    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var executionRepository: ExecutionRepository

    @Autowired
    private lateinit var organizationRepository: OrganizationRepository

    @Autowired
    private lateinit var gitRepository: GitRepository

    @Autowired
    private lateinit var testSuitesSourceRepository: TestSuitesSourceRepository

    @Autowired
    private lateinit var testSuitesSourceService: TestSuitesSourceService

    @Autowired
    private lateinit var testSuitesSourceSnapshotStorage: TestSuitesSourceSnapshotStorage

    @Autowired
    private lateinit var fileStorage: FileStorage

    @TempDir
    internal lateinit var tmpDir: Path
    private lateinit var project: Project
    private lateinit var git: Git
    private lateinit var testSuitesSource: TestSuitesSource
    private lateinit var testSuitesSourceSnapshotKey: TestSuitesSourceSnapshotKey
    private lateinit var standardTestSuitesSourceSnapshotKeys: List<TestSuitesSourceSnapshotKey>
    private lateinit var binFile: Path
    private lateinit var propertyFile: Path

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    @BeforeEach
    fun setup() {
        project = projectRepository.findAll()
            .first { it.name == "huaweiName" }
        git = gitRepository.findAllByOrganizationId(project.organization.requiredId())
            .first { it.url == "github" }
        testSuitesSource = testSuitesSourceRepository.findAllByGit(git)
            .first { it.organization == project.organization }

        binFile = tmpDir.resolve("binFile").apply {
            createFile()
            writeText("binFile")
        }
        propertyFile = tmpDir.resolve("property").apply {
            createFile()
            writeText("property")
        }
        fileStorage.upload(
            ProjectCoordinates(project.organization.name, project.name),
            binFile.toFileInfo().toStorageKey(),
            binFile.toDataBufferFlux().map { it.asByteBuffer() }).subscribeOn(Schedulers.immediate()).block()
        fileStorage.upload(
            ProjectCoordinates(project.organization.name, project.name),
            propertyFile.toFileInfo().toStorageKey(),
            propertyFile.toDataBufferFlux().map { it.asByteBuffer() }).subscribeOn(Schedulers.immediate()).block()

        // snapshot with tests should exist
        testSuitesSourceSnapshotKey = TestSuitesSourceSnapshotKey(
            testSuitesSource.toDto(),
            "123",
            Instant.now().toEpochMilli()
        )
        createTempSnapshot(testSuitesSourceSnapshotKey)

        standardTestSuitesSourceSnapshotKeys = testSuitesSourceService.getStandardTestSuitesSources()
            .map { it.toDto() }
            .map {
                TestSuitesSourceSnapshotKey(
                    it,
                    "123",
                    Instant.now().toEpochMilli()
                )
            }
            .onEach { createTempSnapshot(it) }
    }

    private fun createTempSnapshot(storageKey: TestSuitesSourceSnapshotKey) {
        val tempFile = createTempFile()
        tempFile.writeText("TEST")
        testSuitesSourceSnapshotStorage.upload(storageKey, tempFile.toByteBufferFlux())
            .subscribeOn(Schedulers.immediate())
            .block()
        tempFile.deleteExisting()
    }

    @AfterEach
    fun cleanup() {
        fileStorage.delete(
            ProjectCoordinates(project.organization.name, project.name),
            binFile.toFileInfo().toStorageKey()
        ).subscribeOn(Schedulers.immediate()).block()
        fileStorage.delete(
            ProjectCoordinates(project.organization.name, project.name),
            propertyFile.toFileInfo().toStorageKey()
        ).subscribeOn(Schedulers.immediate()).block()
        binFile.deleteIfExists()
        propertyFile.deleteIfExists()

        standardTestSuitesSourceSnapshotKeys.forEach {
            testSuitesSourceSnapshotStorage.delete(it)
                .subscribeOn(Schedulers.immediate())
                .block()
        }
        testSuitesSourceSnapshotStorage.delete(testSuitesSourceSnapshotKey)
            .subscribeOn(Schedulers.immediate())
            .block()
    }

    @Suppress("TOO_LONG_FUNCTION", "LongMethod")
    @Test
    @WithMockUser(username = "admin")
    fun checkSaveProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val sdk = Jdk("8")
        mockServerOrchestrator.enqueue(
            "/initializeAgents",
            MockResponse()
                .setResponseCode(202)
                .setBody("Clone pending")
                .addHeader("Content-Type", "application/json")
        )
        val runExecutionRequest = RunExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = project.organization.name,
                projectName = project.name,
            ),
            testSuiteIds = listOf(1L, 2L),
            files = emptyList(),
            sdk = sdk,
            execCmd = null,
            batchSizeForAnalyzer = null,
        )

        webClient.post()
            .uri("/api/$v1/run/trigger?testingType={testingType}", TestingType.PRIVATE_TESTS.name)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(runExecutionRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.ACCEPTED)
            .expectBody<String>()
            .consumeWith { result ->
                Assertions.assertNotNull(result.responseBody)
                result.responseBody?.run {
                    Assertions.assertTrue(startsWith("Clone pending, execution id is")) {
                        "Invalid responseBody $this"
                    }
                }
            }
        executionRepository.findAll().shouldExist {
            it.project.name == project.name &&
                    it.project.organization == project.organization &&
                    it.type == TestingType.PRIVATE_TESTS &&
                    it.sdk == sdk.toString()
        }
    }

    @Test
    @WithMockUser(username = "admin")
    fun checkNonExistingProject() {
        mutateMockedUser {
            details = AuthenticationDetails(id = 1)
        }

        val sdk = Jdk("11")
        val organization = organizationRepository.getOrganizationById(1)
        val runExecutionRequest = RunExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = organization.name,
                projectName = "INVALID",
            ),
            testSuiteIds = listOf(1L, 2L, 3L),
            files = listOf(
                propertyFile.toFileInfo().toStorageKey(),
                binFile.toFileInfo().toStorageKey(),
            ),
            sdk = sdk,
            execCmd = null,
            batchSizeForAnalyzer = null,
        )
        webClient.post()
            .uri("/api/$v1/run/trigger?testingType={testingType}", TestingType.PRIVATE_TESTS.name)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(runExecutionRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_FOUND)
    }

    companion object {
        @JvmStatic lateinit var mockServerOrchestrator: MockWebServer

        @AfterEach
        fun cleanup() {
            mockServerOrchestrator.checkQueues()
            mockServerOrchestrator.cleanup()
        }

        @AfterAll
        fun tearDown() {
            mockServerOrchestrator.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            mockServerOrchestrator = createMockWebServer()
            mockServerOrchestrator.start()
            registry.add("backend.orchestratorUrl") { "http://localhost:${mockServerOrchestrator.port}" }
        }
    }
}
