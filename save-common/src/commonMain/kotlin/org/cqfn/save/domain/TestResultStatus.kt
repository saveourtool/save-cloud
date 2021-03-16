package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of results status
 */
@Serializable
enum class TestResultStatus {
    /**
     * Test completed successfully
     */
    PASSED,

    /**
     * Test failed
     */
    FAILED,

    /**
     * Test ignore
     */
    IGNORED,

    /**
     * CLI exit 1
     */
    INTERNAL_ERROR,

    /**
     * CLI exit 42
     */
    TEST_ERROR,
    ;
}
