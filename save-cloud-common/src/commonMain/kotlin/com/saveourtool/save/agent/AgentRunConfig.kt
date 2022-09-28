package com.saveourtool.save.agent

import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.test.TestDto
import kotlinx.serialization.Serializable

/**
 * @property tests a list of new jobs for this agent
 * @property cliArgs command line arguments for SAVE launch
 * @property executionDataUploadUrl an url to upload execution data ([List] of [TestExecutionDto])
 * @property debugInfoUploadUrl an url to upload debug info ([TestResultDebugInfo])
 */
@Serializable
data class AgentRunConfig(
    val tests: List<TestDto>,
    val cliArgs: String,
    val executionDataUploadUrl: String,
    val debugInfoUploadUrl: String,
)