package com.saveourtool.save.entities

import com.saveourtool.save.domain.FileKey
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.Sdk
import com.saveourtool.save.execution.TestingType
import kotlinx.serialization.Serializable

/**
 * @property projectCoordinates project coordinates for evaluated tool
 * @property testSuiteIds selected test suites for running
 * @property files files of evaluated tool
 * @property sdk
 * @property execCmd
 * @property batchSizeForAnalyzer
 */
@Serializable
data class RunExecutionRequest(
    val projectCoordinates: ProjectCoordinates,

    val testSuiteIds: List<Long>,
    val files: List<FileKey>,

    val sdk: Sdk,
    val execCmd: String? = null,
    val batchSizeForAnalyzer: String? = null,

    val testingType: TestingType,
    val contestName: String? = null,
) {
    init {
        require((testingType == TestingType.CONTEST_MODE) xor (contestName == null)) {
            "RunExecutionRequest.contestName shouldn't be set unless testingType is ${TestingType.CONTEST_MODE}"
        }
    }
}
