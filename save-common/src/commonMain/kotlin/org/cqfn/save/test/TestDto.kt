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
