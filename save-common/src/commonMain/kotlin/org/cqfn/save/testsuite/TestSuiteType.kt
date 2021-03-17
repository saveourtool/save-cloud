package org.cqfn.save.testsuite

import kotlinx.serialization.Serializable

@Serializable
enum class TestSuiteType {
    /**
     * Type Project
     */
    PROJECT,

    /**
     * Type Standard
     */
    STANDARD,
}
