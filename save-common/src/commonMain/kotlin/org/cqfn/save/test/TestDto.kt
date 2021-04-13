package org.cqfn.save.test

import kotlinx.serialization.Serializable

/**
 * @property expectedFilePath
 * @property testFilePath
 * @property testSuiteId
 * @property id
 */
@Serializable
data class TestDto(
    var expectedFilePath: String,
    var testFilePath: String,
    var testSuiteId: Long,
    var id: Long
)
