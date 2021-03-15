package org.cqfn.save.test_status

import kotlinx.serialization.Serializable

@Serializable
enum class TestResultStatus {
    /**
     * Test passed execution
     */
    PASSED,

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
     * Some error happened in the test (CLI exit 42)
     */
    TEST_ERROR,
}
