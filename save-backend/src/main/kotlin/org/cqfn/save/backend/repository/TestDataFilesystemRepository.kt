package org.cqfn.save.backend.repository

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.entities.TestExecution

import com.fasterxml.jackson.databind.ObjectMapper
import okio.Path.Companion.toPath
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name

/**
 * A repository for storing additional data associated with test results
 */
@Repository
class TestDataFilesystemRepository(configProperties: ConfigProperties,
                                   private val objectMapper: ObjectMapper,
) {
    /**
     * Root directory for storing test data
     */
    internal val root: Path = (Paths.get(configProperties.fileStorage.location) / "debugInfo").apply {
        if (!exists()) {
            createDirectories()
        }
    }

    /**
     * Store provided [testResultDebugInfo] associated with [executionId]
     *
     * @param executionId
     * @param testResultDebugInfo
     */
    fun save(
        executionId: Long,
        testResultDebugInfo: TestResultDebugInfo,
    ) {
        with(testResultDebugInfo) {
            val destination = testResultLocation.toFsResource(executionId).file
            destination.parentFile.mkdirs()
            objectMapper.writeValue(
                destination,
                testResultDebugInfo
            )
        }
    }

    private fun TestResultLocation.toFsResource(executionId: Long) = FileSystemResource(
        getLocation(executionId, this)
    )

    /**
     * Get location of additional data for [testExecution]
     *
     * @param testExecution a `TestExecution` that exists in the DB
     * @return path to file with additional data
     */
    @Suppress("UnsafeCallOnNullableType")
    @Transactional
    fun getLocation(testExecution: TestExecution): Path {
        val test = testExecution.test
        val path = Paths.get(test.filePath)
        val testResultLocation = TestResultLocation(test.testSuite.name, test.pluginName, path.parent.toString(), path.name)
        return getLocation(testExecution.execution.id!!, testResultLocation)
    }

    /**
     * Get location of additional data for [testExecutionDto]
     *
     * @param executionId
     * @param testExecutionDto
     * @return path to file with additional data
     */
    @Suppress("UnsafeCallOnNullableType")
    fun getLocation(executionId: Long, testExecutionDto: TestExecutionDto): Path {
        val path = testExecutionDto.filePath.toPath()
        val testResultLocation = TestResultLocation(testExecutionDto.testSuiteName!!, testExecutionDto.pluginName, path.parent.toString(), path.name)
        return getLocation(executionId, testResultLocation)
    }

    /**
     * @param executionId
     * @param testResultLocation
     * @return path to file with additional data
     */
    internal fun getLocation(executionId: Long, testResultLocation: TestResultLocation) = with(testResultLocation) {
        root / executionId.toString() / pluginName / testSuiteName / testLocation / "$testName-debug.json"
    }
}
