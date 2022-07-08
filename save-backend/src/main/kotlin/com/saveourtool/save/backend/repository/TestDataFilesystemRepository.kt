package com.saveourtool.save.backend.repository

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.execution.ExecutionUpdateDto

import com.fasterxml.jackson.databind.ObjectMapper
import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Repository

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists

/**
 * A repository for storing additional data associated with test results
 */
@Repository
class TestDataFilesystemRepository(
    configProperties: ConfigProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(TestDataFilesystemRepository::class.java)

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
            log.debug("Writing debug info for $executionId to $destination")
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
     * Get location of execution data for given [executionId]
     *
     * @param executionId
     * @return path to file with execution info
     */
    fun getExecutionInfoFile(executionId: Long) = FileSystemResource(
        root / executionId.toString() / "execution-info.json"
    )

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
        val testResultLocation = TestResultLocation(testExecutionDto.testSuiteName!!, testExecutionDto.pluginName,
            path.parent.toString(), path.name)
        return getLocation(executionId, testResultLocation)
    }

    /**
     * @param executionId
     * @param testResultLocation
     * @return path to file with additional data
     */
    internal fun getLocation(executionId: Long, testResultLocation: TestResultLocation) = with(testResultLocation) {
        root / executionId.toString() / pluginName / sanitizePathName(testSuiteName) / testLocation / "$testName-debug.json"
    }

    /**
     * remove non valid chars from path to work on windows
     */
    private fun sanitizePathName(name: String): String =
            name.replace("[\\\\/:*?\"<>| ]".toRegex(), "")

    /**
     * @param executionInfo
     */
    fun save(executionInfo: ExecutionUpdateDto) {
        val destination = getExecutionInfoFile(executionInfo.id).file
        if (destination.exists()) {
            val existingExecutionInfo = loadExecutionInfo(destination)
            save(existingExecutionInfo.copy(
                failReason = "${existingExecutionInfo.failReason}, ${executionInfo.failReason}"),
                destination)
        } else {
            destination.parentFile.mkdirs()
            save(executionInfo, destination)
        }
    }

    private fun save(executionInfo: ExecutionUpdateDto, destination: File) {
        log.debug("Writing debug info for ${executionInfo.id} to $destination")
        objectMapper.writeValue(
            destination,
            executionInfo
        )
    }

    private fun loadExecutionInfo(destination: File): ExecutionUpdateDto {
        TODO("Not yet implemented")
    }

    /**
     * @param executionId
     * @return
     */
    fun getLocation(executionId: Long): Any {
        TODO("Not yet implemented")
    }
}
