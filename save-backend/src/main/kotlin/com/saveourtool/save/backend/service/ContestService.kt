package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.ContestRepository
import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.ContestStatus
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

/**
 * Service for contests
 *
 * @property contestRepository
 */
@Service
class ContestService(
    private val contestRepository: ContestRepository,
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
}
