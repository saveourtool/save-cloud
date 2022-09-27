package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.*
import com.saveourtool.save.permission.Rights
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository of [LnkOrganizationTestSuite]
 */
@Repository
interface LnkOrganizationTestSuiteRepository : BaseEntityRepository<LnkOrganizationTestSuite> {
    /**
     * @param organization
     * @return [LnkOrganizationTestSuite] by [organization]
     */
    fun findByOrganization(organization: Organization): List<LnkOrganizationTestSuite>

    /**
     * @param organization
     * @param testSuite
     * @return [LnkUserProject] by [organization] and [testSuite]
     */
    fun findByOrganizationAndTestSuite(organization: Organization, testSuite: TestSuite): LnkOrganizationTestSuite?

    /**
     * @param organization
     * @param rights
     * @return List of [LnkOrganizationTestSuite] where [organization] has [rights]
     */
    fun findByOrganizationAndRights(organization: Organization, rights: Rights): List<LnkOrganizationTestSuite>

    /**
     * @param organizationId
     * @param testSuiteId
     * @return [LnkOrganizationTestSuite] by [organizationId] and [testSuiteId]
     */
    fun findByOrganizationIdAndTestSuiteId(organizationId: Long, testSuiteId: Long): LnkOrganizationTestSuite?

    /**
     * Save [LnkOrganizationTestSuite] using only ids and rights string.
     *
     * @param organizationId
     * @param testSuiteId
     * @param rights
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_organization_test_suite (organization_id, test_suite_id, rights) values (:organization_id, :test_suite_id, :rights)",
        nativeQuery = true,
    )
    fun save(
        @Param("organization_id") organizationId: Long,
        @Param("test_suite_id") testSuiteId: Long,
        @Param("rights") rights: String
    )
}
