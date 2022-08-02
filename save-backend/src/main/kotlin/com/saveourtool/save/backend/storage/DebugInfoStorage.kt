package com.saveourtool.save.backend.storage

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.utils.toFluxByteBufferAsJson
import com.saveourtool.save.domain.DebugInfoStorageKey
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.domain.TestResultLocation
import com.saveourtool.save.storage.AbstractFileBasedStorage
import com.saveourtool.save.utils.countPartsTill
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.getLogger
import com.saveourtool.save.utils.pathNamesTill

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
    AbstractFileBasedStorage<DebugInfoStorageKey>(Path.of(configProperties.fileStorage.location) / "debugInfo") {
    /**
     * @param rootDir
     * @param pathToContent
     * @return true if path endsWith [SUFFIX_FILE_NAME]
     */
    override fun isKey(rootDir: Path, pathToContent: Path): Boolean =
            pathToContent.name.endsWith(SUFFIX_FILE_NAME) && pathToContent.countPartsTill(rootDir) == PATH_PARTS_COUNT

    /**
     * @param rootDir
     * @param pathToContent
     * @return [Pair] of executionId and [TestResultLocation] object is built by [Path]
     */
    @Suppress("MAGIC_NUMBER", "MagicNumber")
    override fun buildKey(rootDir: Path, pathToContent: Path): DebugInfoStorageKey {
        val pathNames = pathToContent.pathNamesTill(rootDir)

        val testName = pathNames[0].dropLast(SUFFIX_FILE_NAME.length)
        val testLocation = pathNames[1]
        val testSuiteName = pathNames[2]
        val pluginName = pathNames[3]
        val executionId = pathNames[4].toLong()
        return DebugInfoStorageKey(
            executionId,
            TestResultLocation(testSuiteName, pluginName, testLocation, testName)
        )
    }

    /**
     * @param rootDir
     * @param key
     * @return [Path] is built by executionId and [TestResultLocation] object
     */
    override fun buildPathToContent(rootDir: Path, key: DebugInfoStorageKey): Path = with(key.testResultLocation) {
        rootDir / key.executionId.toString() / pluginName / sanitizePathName(testSuiteName) / testLocation / "$testName$SUFFIX_FILE_NAME"
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
            return upload(DebugInfoStorageKey(executionId, testResultLocation), testResultDebugInfo.toFluxByteBufferAsJson(objectMapper))
        }
    }

    /**
     * remove non valid chars from path to work on windows
     */
    private fun sanitizePathName(name: String): String =
            name.replace("[\\\\/:*?\"<>| ]".toRegex(), "")

    companion object {
        private val log: Logger = getLogger<DebugInfoStorage>()
        private const val PATH_PARTS_COUNT = 5
        private const val SUFFIX_FILE_NAME = "-debug.json"
    }
}
