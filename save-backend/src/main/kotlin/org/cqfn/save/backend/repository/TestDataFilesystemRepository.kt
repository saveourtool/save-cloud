package org.cqfn.save.backend.repository

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.entities.TestExecution
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.name

@Repository
class TestDataFilesystemRepository(configProperties: ConfigProperties) {
    internal val root: Path = (Paths.get(configProperties.fileStorage.location) / "debugInfo").apply {
        if (!exists()) {
            createDirectories()
        }
    }

    /**
     * Store provided [testResultDebugInfo] associated with [executionId]
     */
    fun save(
        executionId: Long,
        testResultDebugInfo: TestResultDebugInfo,
    ) {
        with (testResultDebugInfo) {
            val destination = testResultLocation.toFsResource(executionId).file
            destination.parentFile.mkdirs()
            destination.writeText(
                Json.encodeToString(DebugInfo(stdout, stderr, durationMillis))
            )
        }
    }

    private fun TestResultLocation.toFsResource(executionId: Long): FileSystemResource {
        return FileSystemResource(
            getLocation(executionId, this)
        )
    }

    @Transactional
    fun getLocation(testExecution: TestExecution): Path {
        val test = testExecution.test
        val path = Paths.get(test.filePath)
        val testResultLocation = TestResultLocation(test.testSuite.name, test.pluginName, path.parent.toString(), path.name)
        return getLocation(testExecution.executionId, testResultLocation)
    }

    fun getLocation(executionId: Long, testResultLocation: TestResultLocation): Path {
        return with(testResultLocation) {
            root / "$executionId" / pluginName / testSuiteName / testLocation / "$testName-debug.json"
        }
    }
}