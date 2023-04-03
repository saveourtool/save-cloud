package com.saveourtool.save.entities

import com.saveourtool.save.permission.Rights
import com.saveourtool.save.testsuite.TestSuiteDto
import kotlinx.serialization.Serializable

/**
 * @property organization organization that is connected to [testSuite]
 * @property testSuite manageable test suite
 * @property rights [Rights] that [organization] has over [testSuite]
 */
@Serializable
data class LnkOrganizationTestSuiteDto(
    val organization: OrganizationDto,
    val testSuite: TestSuiteDto,
    val rights: Rights,
)
