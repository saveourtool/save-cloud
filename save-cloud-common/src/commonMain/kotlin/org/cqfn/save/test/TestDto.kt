/**
 * DTOs for retrieving test batches
 */

package org.cqfn.save.test

import kotlinx.serialization.Serializable

/**
 * @property filePath path to a test file
 * @property hash hash of file content
 * @property testSuiteId id of test suite, which this test belongs to
 * @property pluginName name of a plugin which this test belongs to
 * @property tags list of tags of current test
 */
@Serializable
data class TestDto(
    val filePath: String,
    val pluginName: String,
    val testSuiteId: Long,
    val hash: String,
    val tags: String,
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
