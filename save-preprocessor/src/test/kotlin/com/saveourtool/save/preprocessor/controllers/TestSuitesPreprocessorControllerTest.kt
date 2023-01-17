package com.saveourtool.save.preprocessor.controllers

import com.saveourtool.save.entities.GitDto
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.preprocessor.service.*
import com.saveourtool.save.preprocessor.utils.GitCommitInfo
import com.saveourtool.save.test.TestsSourceSnapshotInfo
import com.saveourtool.save.test.TestsSourceVersionInfo
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.testsuite.TestSuitesSourceFetchMode
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.invocation.InvocationOnMock
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
    private val tag = "v0.1"
    private val branch = "main"
    private val commit = "1234567"
    private val fullCommit = "1234567890"

    @BeforeEach
    fun setup() {
        val answerCall = { answer: InvocationOnMock ->
            @Suppress("UNCHECKED_CAST")
            val processor = answer.arguments[2] as GitRepositoryProcessor<TestSuiteList>
            processor(repositoryDirectory, GitCommitInfo(fullCommit, creationTime))
        }
        doAnswer(answerCall).whenever(gitPreprocessorService)
            .cloneTagAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(tag), any())
        doAnswer(answerCall).whenever(gitPreprocessorService)
            .cloneBranchAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(branch), any())
        doAnswer(answerCall).whenever(gitPreprocessorService)
            .cloneCommitAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(commit), any())
        doAnswer { answer ->
            @Suppress("UNCHECKED_CAST")
            val processor = answer.arguments[1] as ArchiveProcessor<TestSuiteList>
            processor(testLocations)
        }.whenever(gitPreprocessorService).archiveToTar<TestSuiteList>(eq(testLocations), any())

        whenever(testsPreprocessorToBackendBridge.saveTestsSuiteSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit, creationTime), any()))
            .thenReturn(Mono.just(Unit))
        whenever(testsPreprocessorToBackendBridge.saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto)))
            .thenReturn(Mono.just(Unit))

        whenever(testDiscoveringService.detectAndSaveAllTestSuitesAndTests(eq(repositoryDirectory), eq(testSuitesSourceDto), any()))
            .thenReturn(Mono.just(emptyList()))
    }

    @Test
    fun fetchFromTagSuccessful() {
        whenever(testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit)))
            .thenReturn(Mono.just(false))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, TestSuitesSourceFetchMode.BY_TAG, tag)
            .block()

        verify(gitPreprocessorService).cloneTagAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(tag), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto, tag))
        verifyNewCommit()
    }

    @Test
    fun fetchFromTagAlreadyContains() {
        whenever(testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit)))
            .thenReturn(Mono.just(true))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, TestSuitesSourceFetchMode.BY_TAG, tag)
            .block()

        verify(gitPreprocessorService).cloneTagAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(tag), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto, tag))
        verifyExistedCommit()
    }

    @Test
    fun fetchFromBranchSuccessful() {
        whenever(testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit)))
            .thenReturn(Mono.just(false))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, TestSuitesSourceFetchMode.BY_BRANCH, branch)
            .block()

        verify(gitPreprocessorService).cloneBranchAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(branch), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto, branch))
        verifyNewCommit()
    }

    @Test
    fun fetchCommitSuccessful() {
        whenever(testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit)))
            .thenReturn(Mono.just(false))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, TestSuitesSourceFetchMode.BY_COMMIT, commit)
            .block()

        verify(gitPreprocessorService).cloneCommitAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(commit), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto, commit))
        verifyNewCommit()
    }

    @Test
    fun fetchCommitAlreadyContains() {
        whenever(testsPreprocessorToBackendBridge.doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit)))
            .thenReturn(Mono.just(true))
        testSuitesPreprocessorController.fetch(testSuitesSourceDto, TestSuitesSourceFetchMode.BY_COMMIT, commit)
            .block()

        verify(gitPreprocessorService).cloneCommitAndProcessDirectory<TestSuiteList>(eq(gitDto), eq(commit), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSourceVersion(testsSourceVersionInfo(testSuitesSourceDto, commit))
        verifyExistedCommit()
    }

    private fun verifyNewCommit() {
        verify(testsPreprocessorToBackendBridge).doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit, creationTime))
        verify(gitPreprocessorService).archiveToTar<TestSuiteList>(eq(testLocations), any())
        verify(testsPreprocessorToBackendBridge).saveTestsSuiteSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit, creationTime), any())
        verify(testDiscoveringService).detectAndSaveAllTestSuitesAndTests(eq(repositoryDirectory), eq(testSuitesSourceDto), eq(fullCommit))
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge, gitPreprocessorService, testDiscoveringService)
    }

    private fun verifyExistedCommit() {
        verify(testsPreprocessorToBackendBridge).doesContainTestsSourceSnapshot(testsSourceSnapshotInfo(testSuitesSourceDto, fullCommit))
        verifyNoMoreInteractions(testsPreprocessorToBackendBridge)
        verifyNoMoreInteractions(gitPreprocessorService)
        verifyNoInteractions(testDiscoveringService)
    }

    private fun testsSourceSnapshotInfo(
        testSuitesSourceDto: TestSuitesSourceDto,
        commitId: String,
        commitTime: Instant? = null,
    ): TestsSourceSnapshotInfo = argThat { value ->
        value.organizationName == testSuitesSourceDto.organizationName &&
                value.sourceName == testSuitesSourceDto.name &&
                value.commitId == commitId &&
                commitTime?.toKotlinInstant()?.toLocalDateTime(TimeZone.UTC).equalsOrTrueIfEmpty(value.commitTime)
    }

    private fun testsSourceVersionInfo(
        testSuitesSourceDto: TestSuitesSourceDto,
        versionNullable: String? = null
    ): TestsSourceVersionInfo = argThat { value ->
        value.snapshotInfo.organizationName == testSuitesSourceDto.organizationName &&
                value.snapshotInfo.sourceName == testSuitesSourceDto.name &&
                versionNullable.equalsOrTrueIfEmpty(value.version)
    }

    private fun <R> R?.equalsOrTrueIfEmpty(anotherValue: R) = this?.let { value -> value == anotherValue } ?: true
}
