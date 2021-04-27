package org.cqfn.save.test

import kotlinx.serialization.Serializable

/**
 * @property filePath
 * @property testSuiteId
 * @property id
 */
@Serializable
data class TestDtoForBatch(
    var filePath: String,
    var testSuiteId: Long,
    var id: Long
)
