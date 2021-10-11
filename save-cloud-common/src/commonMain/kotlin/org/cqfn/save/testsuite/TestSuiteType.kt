package org.cqfn.save.testsuite

import kotlinx.serialization.Serializable

/**
 * Types of test suites
 */
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

    /**
     * Type Obsolete Standard for old test suites
     */
    OBSOLETE_STANDARD,
    ;
}
