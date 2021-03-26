package org.cqfn.save.test

import kotlinx.serialization.Serializable

@Serializable
data class TestDto (
    var expectedFilePath: String,
    var testFilePath: String,
    var testSuiteId: Long,
    var id: String
)
