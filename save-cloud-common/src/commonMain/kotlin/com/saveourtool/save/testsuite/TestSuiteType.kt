package com.saveourtool.save.testsuite

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
    ;
}
