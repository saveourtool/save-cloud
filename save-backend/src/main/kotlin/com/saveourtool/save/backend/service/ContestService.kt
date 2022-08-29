package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.ContestRepository
import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.ContestStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

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
     * @param contestIds
     * @return [Contest]s with ids from [contestIds]
     */
    fun findByIdIn(contestIds: Set<Long>): List<Contest> = contestRepository.findByIdIn(contestIds)

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
     * @param organizationName
     * @param pageable
     * @return page of contests
     */
    fun findPageOfContestsByOrganizationName(organizationName: String, pageable: Pageable) = contestRepository.findByOrganizationName(organizationName, pageable)

    /**
     * @param newContest
     * @return [Contest]
     */
    fun updateContest(newContest: Contest) = contestRepository.save(newContest)

    /**
     * @param newContest
     * @return true if contest was saved, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun createContestIfNotPresent(newContest: Contest): Boolean =
            if (contestRepository.findByName(newContest.name).isEmpty) {
                contestRepository.save(newContest.apply {  })
                true
            } else {
                false
            }

    @Transactional
    @Suppress("AVOID_NULL_CHECKS")
    fun addOrDeleteFeaturedContest(contest: Contest): Long? = contest.requiredId().let { contestId ->
        if (contestRepository.findFeaturedContestById(contestId) == null) {
            contestRepository.saveFeaturedContest(contestId)
            contestId
        } else {
            contestRepository.deleteFeaturedContestById(contestId)
            null
        }
    }

    fun getFeaturedContests(): List<Contest> = findByIdIn(contestRepository.findFeaturedContestIds())

    fun getNewestContests(pageSize: Int): List<Contest> = contestRepository.findByStatusNotAndEndTimeAfterOrderByCreationTimeDesc(
        ContestStatus.DELETED,
        LocalDateTime.now(),
        PageRequest.ofSize(pageSize),
    )
}
