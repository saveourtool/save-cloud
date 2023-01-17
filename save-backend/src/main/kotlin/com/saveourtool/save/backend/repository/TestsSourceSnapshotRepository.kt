package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestsSourceSnapshot]
 */
@Suppress(
    "IDENTIFIER_LENGTH",
    "FUNCTION_NAME_INCORRECT_CASE",
    "FunctionNaming",
    "FunctionName",
)
@Repository
interface TestsSourceSnapshotRepository : BaseEntityRepository<TestsSourceSnapshot> {
    /**
     * @param sourceId
     * @param commitId
     * @return [TestsSourceSnapshot] found by [commitId] in [com.saveourtool.save.entities.TestSuitesSource] (by [sourceId])
     */
    fun findBySource_IdAndCommitId(sourceId: Long, commitId: String): TestsSourceSnapshot?

    /**
     * @param organizationName
     * @param sourceName
     * @param commitId
     * @return [TestsSourceSnapshot] found by [commitId] in [com.saveourtool.save.entities.TestSuitesSource] (by [organizationName], [sourceName])
     */
    fun findBySource_Organization_NameAndSource_NameAndCommitId(
        organizationName: String,
        sourceName: String,
        commitId: String
    ): TestsSourceSnapshot?
}
