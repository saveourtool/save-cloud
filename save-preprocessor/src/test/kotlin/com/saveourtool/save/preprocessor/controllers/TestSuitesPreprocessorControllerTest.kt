package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.*
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import java.nio.file.Paths
import java.time.Instant

typealias TestSuiteList = List<TestSuite>

internal class TestSuitesPreprocessorControllerTest {
    private val gitPreprocessorService: GitPreprocessorService = mock()
    private val testDiscoveringService: TestDiscoveringService = mock()
    private val testsPreprocessorToBackendBridge: TestsPreprocessorToBackendBridge = mock()
    private val testSuitesPreprocessorController = TestSuitesPreprocessorController(
        gitPreprocessorService,
        testDiscoveringService,
        testsPreprocessorToBackendBridge,
    )
    private val gitDto = GitDto("https://github.com/saveourtool/save-cli")
    private val testSuitesSourceDto = TestSuitesSourceDto(
        "Organization",
        "TestSuitesSource",
        null,
        gitDto,
        "examples/discovery-test",
        "aaaaaa",
    )
    private val repositoryDirectory = Paths.get("./some-folder")
    private val testLocations = repositoryDirectory.resolve(testSuitesSourceDto.testRootPath)
    private val creationTime = Instant.now()

    @BeforeEach
    fun setup() {
        doAnswer { answer ->
            @Suppress("UNCHECKED_CAST")
            val processor = answer.arguments[3] as GitRepositoryProcessor<TestSuiteList>
            processor(repositoryDirectory, creationTime)
        }.whenever(gitPreprocessorService).cloneBranchAndProcessDirectory<TestSuiteList>(eq(gitDto), eq("main"),
            any())
        doAnswer { answer ->
            @Suppress("UNCHECKED_CAST")
            val processor = answer.arguments[1] as ArchiveProcessor<TestSuiteList>
            processor(testLocations)
        }.whenever(gitPreprocessorService).archiveToTar<TestSuiteList>(eq(testLocations), any())

        whenever(testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(eq(testSuitesSourceDto), any(), eq(creationTime), any()))
            .thenReturn(Mono.just(Unit))

        whenever(testDiscoveringService.detectAndSaveAllTestSuitesAndTests(eq(repositoryDirectory), eq(testSuitesSourceDto), any()))
            .thenReturn(Mono.just(emptyList()))
    }

    @Test
    fun fetchLatestAlreadyContains() {
        whenever(testsPreprocessorToBackendBridge.doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto), any()))
            .thenReturn(Mono.just(true))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, "tagName")
            .block()

        verify(testsPreprocessorToBackendBridge).doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto), any())
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge)
        verifyNoInteractions(gitPreprocessorService, testDiscoveringService)
    }

    @Test
    fun fetchLatestSuccessful() {
        var version: String = "N/A"
        whenever(testsPreprocessorToBackendBridge.doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto),
            argThat { latestVersion ->
                version = latestVersion
                true
            }))
            .thenReturn(Mono.just(false))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, "tagName")
            .block()

        verify(testsPreprocessorToBackendBridge).doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto), eq(version))
        verify(gitPreprocessorService).cloneBranchAndProcessDirectory<TestSuiteList>(eq(gitDto), eq("main"),
            any())
        verify(gitPreprocessorService).archiveToTar<TestSuiteList>(eq(testLocations), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSuiteSourceSnapshot(eq(testSuitesSourceDto), eq(version), eq(creationTime), any())
        verify(testDiscoveringService).detectAndSaveAllTestSuitesAndTests(eq(repositoryDirectory), eq(testSuitesSourceDto), eq(version))
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge, gitPreprocessorService, testDiscoveringService)
    }

    @Test
    fun fetchSpecificSuccessful() {
        val version = "some"
        whenever(testsPreprocessorToBackendBridge.doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto), eq(version)))
            .thenReturn(Mono.just(false))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, version)
            .block()

        verify(testsPreprocessorToBackendBridge).doesTestSuitesSourceContainVersion(eq(testSuitesSourceDto), eq(version))
        verify(gitPreprocessorService).cloneBranchAndProcessDirectory<TestSuiteList>(eq(gitDto), eq("main"),
            any())
        verify(gitPreprocessorService).archiveToTar<TestSuiteList>(eq(testLocations), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSuiteSourceSnapshot(eq(testSuitesSourceDto), eq(version), eq(creationTime), any())
        verify(testDiscoveringService).detectAndSaveAllTestSuitesAndTests(eq(repositoryDirectory), eq(testSuitesSourceDto), eq(version))
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge, gitPreprocessorService, testDiscoveringService)
    }
}
