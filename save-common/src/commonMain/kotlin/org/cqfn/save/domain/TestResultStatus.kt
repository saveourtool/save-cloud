package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of results status
 */
@Serializable
enum class TestResultStatus {
    /**
     * Ready for testing
     */
    READY,

    /**
     * Running state
     */
    RUNNING,

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
     * Test completed successfully
     */
    PASSED,

    /**
     * CLI exit 42
     */
    TEST_ERROR,
    ;
}
