@file:Suppress("HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE")

package org.cqfn.save

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.domain.TestResultLocation
import java.nio.file.Paths
import kotlin.io.path.name

/**
 * Since in standard mode we create additional directories for correct execution,
 * we will also mark them with such prefix, in aim to correctly match test execution results with data from DB,
 * which didn't know about our actions with creation of additional dirs
 */
const val PREFIX_FOR_SUITES_LOCATION_IN_STANDARD_MODE = "STANDARD_"

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
