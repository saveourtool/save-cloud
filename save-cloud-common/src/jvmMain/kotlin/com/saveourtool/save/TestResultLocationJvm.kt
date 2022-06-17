@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package com.saveourtool.save

import com.saveourtool.save.agent.TestExecutionDto
import com.saveourtool.save.domain.TestResultLocation
import java.nio.file.Paths
import kotlin.io.path.name

/**
 * Factory method to create a [TestResultLocation] from [TestExecutionDto]
 *
 * @param testExecutionDto
 * @return a new [TestResultLocation]
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
