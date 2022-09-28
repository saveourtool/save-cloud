package com.saveourtool.save.entities

import com.saveourtool.save.permission.Rights
import com.saveourtool.save.testsuite.TestSuiteDto

/**
 * @property organization organization that is connected to [testSuite]
 * @property testSuite manageable test suite
 * @property rights [Rights] that [organization] has over [testSuite]
 */
data class LnkOrganizationTestSuiteDto(
    var organization: OrganizationDto,
    var testSuite: TestSuiteDto,
    var rights: Rights,
)
