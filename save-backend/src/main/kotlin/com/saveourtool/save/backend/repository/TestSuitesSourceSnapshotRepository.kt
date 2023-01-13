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
     * @param source
     * @param commitId
     * @return [TestSuitesSourceSnapshot] found by [commitId] in [TestSuitesSource]
     */
    fun findBySourceAndCommitId(source: TestSuitesSource, commitId: String): TestSuitesSourceSnapshot?
}
