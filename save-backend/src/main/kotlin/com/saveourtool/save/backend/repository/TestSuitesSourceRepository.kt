package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.TestSuitesSource
import org.springframework.stereotype.Repository

/**
 * Repository of [TestSuitesSource]
 */
@Repository
interface TestSuitesSourceRepository : BaseEntityRepository<TestSuitesSource> {
    /**
     * @param organizationId
     * @return found entities
     */
    fun findAllByOrganizationId(organizationId: Long): List<TestSuitesSource>

    /**
     * @param organizationId
     * @param name
     * @return found entity or null
     */
    fun findByOrganizationIdAndName(organizationId: Long, name: String): TestSuitesSource?

    /**
     * @param organization
     * @param git
     * @param testRootPath
     * @return found entity or null
     */
    fun findByOrganizationAndGitAndTestRootPath(
        organization: Organization,
        git: Git,
        testRootPath: String
    ): TestSuitesSource?

    /**
     * @param git
     * @return found entities
     */
    fun findAllByGit(git: Git): List<TestSuitesSource>
}
