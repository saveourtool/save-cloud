package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.ContestStatus
import com.saveourtool.save.utils.LocalDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * The repository of contest entities
 */
@Repository
interface ContestRepository : JpaRepository<Contest, Long>, QueryByExampleExecutor<Contest>,
JpaSpecificationExecutor<Contest> {
    /**
     * @param name
     * @return contest by name
     */
    fun findByName(name: String): Optional<Contest>

    /**
     * @param currentTime
     * @param pageable
     * @param status
     * @return [Page] of contests with startTime greater than [currentTime]
     */
    fun findByEndTimeBeforeAndStatus(
        currentTime: LocalDateTime,
        status: ContestStatus,
        pageable: Pageable,
    ): Page<Contest>

    /**
     * @param currentTime
     * @param currentTimeAgain
     * @param pageable
     * @param status
     * @return [Page] of contests with startTime less than [currentTime] and endTime greater than [currentTime]
     */
    fun findByStartTimeBeforeAndEndTimeAfterAndStatus(
        currentTime: LocalDateTime,
        // need to pass current time twice due to spring features
        currentTimeAgain: LocalDateTime,
        status: ContestStatus,
        pageable: Pageable,
    ): Page<Contest>

    /**
     * @param currentTime
     * @param currentTimeAgain
     * @param status
     * @param contestIds
     * @param pageable
     * @return [Page] of active [Contest]s that ids are not in [contestIds]
     */
    fun findByStartTimeBeforeAndEndTimeAfterAndStatusAndIdNotIn(
        currentTime: LocalDateTime,
        currentTimeAgain: LocalDateTime,
        status: ContestStatus,
        contestIds: Set<Long>,
        pageable: Pageable,
    ): Page<Contest>
}
