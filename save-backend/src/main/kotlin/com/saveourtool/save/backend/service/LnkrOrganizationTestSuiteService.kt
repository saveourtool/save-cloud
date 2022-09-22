package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkOrganizationTestSuiteRepository
import com.saveourtool.save.backend.repository.LnkUserOrganizationRepository
import com.saveourtool.save.backend.repository.UserRepository
import com.saveourtool.save.backend.utils.AuthenticationDetails
import com.saveourtool.save.domain.Role
import com.saveourtool.save.entities.*
import com.saveourtool.save.permission.Rights
import com.saveourtool.save.utils.blockingToFlux
import com.saveourtool.save.utils.getHighestRole
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import kotlin.NoSuchElementException

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
     * Set [rights] of organization with [organizationId] over test suite with [testSuiteId].
     *
     * @throws IllegalStateException if [rights] is [Role.NONE]
     */
    @Suppress("KDOC_WITHOUT_PARAM_TAG", "UnsafeCallOnNullableType")
    fun setRightsByIds(testSuiteId: Long, organizationId: Long, rights: Rights) {
        if (rights == Rights.NONE) {
            throw IllegalStateException("NONE rights should not be present in database!")
        }
        lnkOrganizationTestSuiteRepository.findByOrganizationIdAndTestSuiteId(organizationId, testSuiteId)
            ?.apply { this.rights = rights }
            ?.let { lnkOrganizationTestSuiteRepository.save(it) }
            ?: lnkOrganizationTestSuiteRepository.save(organizationId, testSuiteId, rights.toString())
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
        ?: throw NoSuchElementException(
            "Cannot unlink organization with name ${organization.name} from test suite with name ${testSuite.name} because no link was found."
        )
}
