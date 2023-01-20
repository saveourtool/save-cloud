package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repositories for TestSuite
 */
@Repository
interface TestSuiteRepository : BaseEntityRepository<TestSuite> {
    /**
     * @param name name of the test suite
     * @param tags tags of the test suite
     * @param sourceSnapshotId ID of source snapshot of the test suite
     * @return matched test suite
     */
    fun findByNameAndTagsAndSourceSnapshotId(
        name: String,
        tags: String?,
        sourceSnapshotId: Long,
    ): TestSuite?

    /**
     * @param source source of the test suite
     * @param version version of snapshot of source
     * @return matched test suites
     */
    fun findAllBySourceAndVersion(
        source: TestSuitesSource,
        version: String
    ): List<TestSuite>

    /**
     * @param source source of the test suite
     * @return matched test suites
     */
    fun findAllBySource(
        source: TestSuitesSource,
    ): List<TestSuite>

    /**
     * @param organizationName
     * @return List of [TestSuite]s
     */
    fun findBySourceOrganizationName(organizationName: String): List<TestSuite>

    /**
     * @param isPublic flag that indicates if given [TestSuite] is available for every organization or not
     * @return List of [TestSuite]s
     */
    fun findByIsPublic(isPublic: Boolean): List<TestSuite>
}
