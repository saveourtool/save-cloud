package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.utils.toFluxByteBufferAsJson
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import java.nio.file.Path

import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * A storage for storing additional data associated with test results
 */
@Service
class DebugInfoStorage(
    configProperties: ConfigProperties,
    private val objectMapper: ObjectMapper,
) :
    AbstractFileBasedStorage<Pair<Long, TestResultLocation>>(Path.of(configProperties.fileStorage.location) / "debugInfo") {
    /**
     * @param rootDir
     * @param pathToContent
     * @return true if path endsWith [SUFFIX_FILE_NAME]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean = pathToContent.name.endsWith(SUFFIX_FILE_NAME)

    /**
     * @param rootDir
     * @param pathToContent
     * @return [Pair] of executionId and [TestResultLocation] object is built by [Path]
     */
    @Suppress("MAGIC_NUMBER", "MagicNumber")
    override fun buildKey(rootDir: Path, pathToContent: Path): Pair<Long, TestResultLocation> {
        val folderNames = generateSequence(pathToContent, Path::getParent)
            .takeWhile { it != rootDir }
            .map { it.name }
            .toList()
        require(folderNames.size == 5) {
            "Invalid path to debugInfo: $pathToContent"
        }
        val testName = folderNames[0].dropLast(SUFFIX_FILE_NAME.length)
        val testLocation = folderNames[1]
        val testSuiteName = folderNames[2]
        val pluginName = folderNames[3]
        val executionId = folderNames[4].toLong()
        return Pair(
            executionId,
            TestResultLocation(testSuiteName, pluginName, testLocation, testName)
        )
    }

    /**
     * @param rootDir
     * @param key
     * @return [Path] is built by executionId and [TestResultLocation] object
     */
    override fun buildPathToContent(rootDir: Path, key: Pair<Long, TestResultLocation>): Path {
        val (executionId, testResultLocation) = key
        return with(testResultLocation) {
            rootDir / executionId.toString() / pluginName / sanitizePathName(testSuiteName) / testLocation / "$testName$SUFFIX_FILE_NAME"
        }
    }

    /**
     * Store provided [testResultDebugInfo] associated with [executionId]
     *
     * @param executionId
     * @param testResultDebugInfo
     * @return count of saved bytes
     */
    fun save(
        executionId: Long,
        testResultDebugInfo: TestResultDebugInfo,
    ): Mono<Long> {
        with(testResultDebugInfo) {
            log.debug { "Writing debug info for $executionId to $testResultLocation" }
            return upload(Pair(executionId, testResultLocation), testResultDebugInfo.toFluxByteBufferAsJson(objectMapper))
        }
    }

    /**
     * remove non valid chars from path to work on windows
     */
    private fun sanitizePathName(name: String): String =
            name.replace("[\\\\/:*?\"<>| ]".toRegex(), "")

    companion object {
        private val log: Logger = getLogger<DebugInfoStorage>()
        private const val SUFFIX_FILE_NAME = "-debug.json"
    }
}
