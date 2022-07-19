package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.ContestRepository
import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.ContestStatus
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Service for contests
 *
 * @property contestRepository
 */
@OptIn(ExperimentalStdlibApi::class)
@Service
class ContestService(
    private val contestRepository: ContestRepository,
    private val testSuitesService: TestSuitesService,
) {
    /**
     * @param contestId
     * @return contest by id
     */
    fun findById(contestId: Long): Optional<Contest> = contestRepository.findById(contestId)

    /**
     * @param name name of contest
     * @return contest by name
     */
    fun findByName(name: String) = contestRepository.findByName(name)

    /**
     * @param pageSize amount of contests that should be taken
     * @return list of active contests
     */
    fun findContestsInProgress(pageSize: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByStartTimeBeforeAndEndTimeAfterAndStatus(
            it,
            it,
            ContestStatus.CREATED,
            Pageable.ofSize(pageSize),
        )
    }.content

    /**
     * @param pageSize amount of contests that should be taken
     * @return list of active contests
     */
    fun findFinishedContests(pageSize: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByEndTimeBeforeAndStatus(
            it,
            ContestStatus.CREATED,
            Pageable.ofSize(pageSize),
        )
    }.content

    /**
     * @param contestIds set of ids of a [Contest]s
     * @param numberOfRecords amount of records that should be taken from database
     * @return list of active [Contest]s which ids are not from [contestIds]
     */
    fun getAllActiveContestsNotFrom(contestIds: Set<Long>, numberOfRecords: Int): List<Contest> = LocalDateTime.now().let {
        contestRepository.findByStartTimeBeforeAndEndTimeAfterAndStatusAndIdNotIn(
            it,
            it,
            ContestStatus.CREATED,
            contestIds,
            Pageable.ofSize(numberOfRecords),
        ).content
    }

    /**
     * @param contest
     * @return test suite that has public test as its part
     */
    @Suppress("COMPLEX_EXPRESSION")
    fun getTestSuiteForPublicTest(contest: Contest) = contest.getTestSuiteIds()
        .firstOrNull()
        ?.let {
            testSuitesService.findTestSuiteById(it)?.getOrNull()
        }

    /**
     * @param contest
     * @return test suite that has public test as its part
     */
    @Suppress("COMPLEX_EXPRESSION")
    fun getTestSuites(contest: Contest) = contest.getTestSuiteIds()
        .firstOrNull()
        ?.let {
            testSuitesService.findTestSuiteById(it)?.getOrNull()
        }
}
