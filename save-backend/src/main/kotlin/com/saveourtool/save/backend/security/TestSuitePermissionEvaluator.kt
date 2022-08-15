package com.saveourtool.save.backend.security

import com.saveourtool.save.entities.TestSuite
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

/**
 * Class that is capable of assessing user's permissions regarding test suites.
 */
@Component
class TestSuitePermissionEvaluator {
    /**
     * fixme: just a remark for future work
     *
     * @param testSuite test suite that is being requested
     * @param authentication
     * @return true if user with [authentication] can access [testSuite], false otherwise
     */
    fun canAccessTestSuite(
        testSuite: TestSuite,
        authentication: Authentication?
    ) = authentication?.let {
        true
    } ?: false
}
