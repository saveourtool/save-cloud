package com.saveourtool.save.request

import com.saveourtool.save.testsuite.GitLocation
import kotlinx.serialization.Serializable

/**
 * @property gitLocation
 */
@Serializable
data class UploadTestSuiteRequest(
    val gitLocation: GitLocation
)
