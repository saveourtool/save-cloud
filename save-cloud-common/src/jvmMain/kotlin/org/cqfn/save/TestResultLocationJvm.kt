package org.cqfn.save

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultLocation
import java.nio.file.Paths
import kotlin.io.path.name

/**
 * @param testExecutionDto
 * @return
 */
fun TestResultLocation.Companion.from(testExecutionDto: TestExecutionDto): TestResultLocation {
    val path = Paths.get(testExecutionDto.filePath)
    return TestResultLocation(
        testExecutionDto.testSuiteName!!,
        testExecutionDto.pluginName,
        (path.parent ?: ".").toString(),
        path.name,
    )
}
