/**
 * DTOs for retrieving test batches
 */

package org.cqfn.save.test

import kotlinx.serialization.Serializable

/**
 * @property filePath
 * @property hash
 * @property testSuiteId
 */
@Serializable
data class TestDto(
    var filePath: String,
    var testSuiteId: Long,
    var hash: String?,
)

/**
 * @property tests a list of tests in a batch
 * @property suitesToArgs map of test suite IDs to command line arguments for these suites
 */
@Serializable
data class TestBatch(
    val tests: List<TestDto>,
    val suitesToArgs: Map<Long, String>,
)
