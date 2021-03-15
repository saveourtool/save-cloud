package org.cqfn.save.teststatus

import kotlinx.serialization.Serializable

/**
 * Possible test result statuses
 */
@Serializable
enum class TestResultStatus {
    /**
     * Test failed execution
     */
    FAILED,

    /**
     * Test was ignored
     */
    IGNORED,

    /**
     * Internal error happened in the agent (CLI exit 1)
     */
    INTERNAL_ERROR,

    /**
     * Test passed execution
     */
    PASSED,

    /**
     * Some error happened in the test (CLI exit 42)
     */
    TEST_ERROR,
    ;
}
