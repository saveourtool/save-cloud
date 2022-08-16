package com.saveourtool.save.kafka

import com.saveourtool.save.test.TestDto

data class TestExecutionTaskDto(
    val tests: List<TestDto>,
    val cliArgs: String,
    override val messageId: String? = null
): KafkaMsg