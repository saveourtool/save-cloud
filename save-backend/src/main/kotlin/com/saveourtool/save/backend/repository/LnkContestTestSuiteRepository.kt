package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.LnkContestTestSuite
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkContestTestSuite]
 */
@Repository
interface LnkContestTestSuiteRepository : BaseEntityRepository<LnkContestTestSuite> {
    /**
     * @param contest
     * @return [LnkContestTestSuite] by [contest]
     */
    fun findByContest(contest: Contest): List<LnkContestTestSuite>

    /**
     * @param contestId
     * @return [LnkContestTestSuite] by [contestId]
     */
    fun findByContestId(contestId: Long): List<LnkContestTestSuite>

    /**
     * @param testSuiteId
     * @return [LnkContestTestSuite] by [testSuiteId]
     */
    fun findByTestSuiteId(testSuiteId: Long): List<LnkContestTestSuite>

    /**
     * @param testSuiteIds [Collection] of testSuite ids
     * @return [LnkContestTestSuite] by [testSuiteIds]
     */
    fun findByTestSuiteIdIn(testSuiteIds: Collection<Long>): List<LnkContestTestSuite>
}
