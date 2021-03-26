package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of results status
 */
@Serializable
enum class TestResultStatus {
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
     * Ready for testing
     */
    READY,

    /**
     * Running state
     */
    RUNNING,

    /**
     * CLI exit 42
     */
    TEST_ERROR,
    ;
}
