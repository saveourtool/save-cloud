package com.saveourtool.save.backend.service

import com.saveourtool.save.test.analysis.api.TestId
import com.saveourtool.save.test.analysis.api.TestIdGenerator
import com.saveourtool.save.test.analysis.api.testId
import com.saveourtool.save.test.analysis.entities.TestExecutionMetadata
import org.springframework.stereotype.Service

/**
 * The high-level test id generator service.
 *
 * @see TestIdGenerator
 */
@Service
class TestIdGeneratorService {
    private val testIdGenerator: TestIdGenerator = TestIdGenerator()

    /**
     * Generates a unique test id.
     *
     * @param metadata test execution metadata.
     * @return the generated unique test id.
     * @see TestIdGenerator.testId
     */
    fun testId(metadata: TestExecutionMetadata): TestId =
            testIdGenerator.testId(metadata)
}
