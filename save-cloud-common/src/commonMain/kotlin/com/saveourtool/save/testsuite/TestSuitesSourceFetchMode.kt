package com.saveourtool.save.testsuite

/**
 * Enum that represents different modes of [TestSuitesSourceDto] fetching
 */
enum class TestSuitesSourceFetchMode {
    BY_BRANCH,
    BY_COMMIT,
    BY_TAG,

    /**
     * temporary added till migration is done
     */
    UNKNOWN,
    ;
}
