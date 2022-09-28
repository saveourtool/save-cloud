@file:Suppress("FILE_NAME_MATCH_CLASS")

package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkOrganizationTestSuiteRepository
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.permission.Rights
import com.saveourtool.save.utils.orNotFound
import org.springframework.stereotype.Service

/**
 * Service of [LnkOrganizationTestSuite]
 */
@Service
class LnkOrganizationTestSuiteService(
    private val lnkOrganizationTestSuiteRepository: LnkOrganizationTestSuiteRepository,
) {
    /**
     * @param organization
     * @return all [TestSuite]s with rights for [organization]
     */
    fun getAllTestSuitesByOrganization(organization: Organization) =
            lnkOrganizationTestSuiteRepository.findByOrganization(organization)
                .map {
                    it.testSuite
                }

    /**
     * @param organization
     * @param testSuite
     * @return [LnkOrganizationTestSuite] by [organization] and [testSuite]
     */
    fun findByOrganizationAndTestSuite(organization: Organization, testSuite: TestSuite) =
            lnkOrganizationTestSuiteRepository.findByOrganizationAndTestSuite(organization, testSuite)

    /**
     * Set [rights] of [organization] over [testSuite].
     *
     * @throws IllegalStateException if [rights] is [Role.NONE]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun setRights(organization: Organization, testSuite: TestSuite, rights: Rights) {
        if (rights == Rights.NONE) {
            throw IllegalStateException("NONE rights should not be present in database!")
        }
        val lnkOrganizationTestSuite = findByOrganizationAndTestSuite(organization, testSuite)
            ?.apply { this.rights = rights }
            ?: LnkOrganizationTestSuite(organization, testSuite, rights)
        lnkOrganizationTestSuiteRepository.save(lnkOrganizationTestSuite)
    }

    /**
     * @param organization
     * @param testSuite
     * @return [Rights] of [organization] over [testSuite]
     */
    fun getRights(organization: Organization, testSuite: TestSuite) =
            findByOrganizationAndTestSuite(organization, testSuite)?.rights ?: Rights.NONE

    /**
     * Removes rights of [organization] over [testSuite].
     *
     * @param organization
     * @param testSuite
     * @return Unit
     */
    @Suppress("UnsafeCallOnNullableType")
    fun removeRights(organization: Organization, testSuite: TestSuite) = findByOrganizationAndTestSuite(organization, testSuite)
        ?.requiredId()
        ?.let { lnkOrganizationTestSuiteRepository.deleteById(it) }
        .orNotFound {
            "Cannot unlink organization with name ${organization.name} from test suite with name ${testSuite.name} because no link was found."
        }
}
