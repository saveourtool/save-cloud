package com.saveourtool.save.request

import com.saveourtool.save.test.TestDto
import com.saveourtool.save.testsuite.TestSuitesSourceSnapshotKey
import kotlinx.serialization.Serializable

/**
 * @property test [TestDto] of a test that is requested
 * @property storageKey storage key with which tests snapshot is written
 */
@Serializable
data class TestFilesRequest(
    val test: TestDto,
    val storageKey: TestSuitesSourceSnapshotKey,
)