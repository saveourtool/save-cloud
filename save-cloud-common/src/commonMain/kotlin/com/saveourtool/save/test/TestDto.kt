/**
 * DTOs for retrieving test batches
 */

package com.saveourtool.save.test

import com.saveourtool.save.domain.PluginType
import com.saveourtool.save.domain.toPluginType
import com.saveourtool.save.testsuite.TestSuitesSourceDto
import com.saveourtool.save.utils.DATABASE_DELIMITER
import kotlinx.serialization.Serializable

/**
 * [List] of [TestDto]
 */
typealias TestBatch = List<TestDto>

/**
 * @property filePath path to a test file
 * @property hash hash of file content
 * @property testSuiteId id of test suite, which this test belongs to
 * @property pluginName name of a plugin which this test belongs to
 * @property additionalFiles
 */
@Serializable
data class TestDto(
    val filePath: String,
    val pluginName: String,
    val testSuiteId: Long,
    val hash: String,
    val additionalFiles: List<String> = emptyList(),
) {
    /**
     * @return [additionalFiles] as a [String]
     */
    fun joinAdditionalFiles() = additionalFiles.joinToString(DATABASE_DELIMITER)
}

/**
 * @property test [TestDto] of a test that is requested
 * @property testSuitesSource source of test
 * @property version version of this test
 */
@Serializable
data class TestFilesRequest(
    val test: TestDto,
    val testSuitesSource: TestSuitesSourceDto,
    val version: String,

)

/**
 * @return [List] of plugin names
 */
fun List<TestDto>.collectPluginNames() = map { it.pluginName }
    .distinct()
    .map { it.toPluginType() }
    .filter { it != PluginType.GENERAL }
