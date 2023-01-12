package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestSuitesSourceSnapshot
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestSuitesSourceSnapshot]
 */
@Repository
interface TestSuitesSourceSnapshotRepository : BaseEntityRepository<TestSuitesSourceSnapshot> {
    /**
     * @param testSuitesSource
     * @param commitId
     * @return [TestSuitesSourceSnapshot] found by [commitId] in [TestSuitesSource]
     */
    fun findBySourceAndCommitId(testSuitesSource: TestSuitesSource, commitId: String): TestSuitesSourceSnapshot?
}
