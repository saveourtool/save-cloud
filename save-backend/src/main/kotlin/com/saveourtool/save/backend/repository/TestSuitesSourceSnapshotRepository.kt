package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.TestSuitesSource
import com.saveourtool.save.entities.TestsSourceSnapshot
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository for [TestsSourceSnapshot]
 */
@Repository
interface TestSuitesSourceSnapshotRepository : BaseEntityRepository<TestsSourceSnapshot> {
    /**
     * @param source
     * @param commitId
     * @return [TestsSourceSnapshot] found by [commitId] in [TestSuitesSource]
     */
    fun findBySourceAndCommitId(source: TestSuitesSource, commitId: String): TestsSourceSnapshot?
}
