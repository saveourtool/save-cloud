package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
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
     * @return [TestsSourceSnapshot] found by [commitId] in [TestSuitesSource] (by [sourceId])
     */
    fun findBySourceIdAndCommitId(sourceId: Long, commitId: String): TestsSourceSnapshot?

    /**
     * @param organizationName name from [com.saveourtool.save.entities.Organization]<-[com.saveourtool.save.entities.TestSuitesSource]
     * @return all [TestsSourceSnapshot] found by provided values
     */
    fun findAllBySource_Organization_Name(
        organizationName: String,
    ): List<TestsSourceSnapshot>

    /**
     * @param source [com.saveourtool.save.entities.TestSuitesSource]
     * @return all [TestsSourceSnapshot] found by provided values
     */
    fun findAllBySource(
        source: TestSuitesSource,
    ): List<TestsSourceSnapshot>

    /**
     * @param organizationName name from [com.saveourtool.save.entities.Organization]<-[com.saveourtool.save.entities.TestSuitesSource]
     * @param sourceName name from [com.saveourtool.save.entities.TestSuitesSource]
     * @return all [TestsSourceSnapshot] found by provided values
     */
    fun findAllBySource_Organization_NameAndSource_Name(
        organizationName: String,
        sourceName: String,
    ): List<TestsSourceSnapshot>

    /**
     * @param organizationName name from [com.saveourtool.save.entities.Organization]<-[com.saveourtool.save.entities.TestSuitesSource]
     * @param sourceName name from [com.saveourtool.save.entities.TestSuitesSource]
     * @param commitId
     * @return [TestsSourceSnapshot] found by provided values
     */
    fun findBySource_Organization_NameAndSource_NameAndCommitId(
        organizationName: String,
        sourceName: String,
        commitId: String,
    ): TestsSourceSnapshot?
}
