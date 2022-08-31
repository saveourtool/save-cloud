package com.saveourtool.save.kafka

import com.saveourtool.save.test.TestDto

/**
 * @property tests
 * @property cliArgs
 * @property messageId
 */
data class TestExecutionTaskDto(
    val tests: List<TestDto>,
    val cliArgs: String,
    override val messageId: String? = null
) : KafkaMsg
