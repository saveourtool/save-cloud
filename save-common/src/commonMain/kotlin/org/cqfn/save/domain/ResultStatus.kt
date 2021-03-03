package org.cqfn.save.domain

import kotlinx.serialization.Serializable

/**
 * Enum of results status
 */
@Serializable
enum class ResultStatus {
    /**
     * Test completed successfully
     */
    DONE,

    /**
     * Test failed
     */
    FAILED,
}
