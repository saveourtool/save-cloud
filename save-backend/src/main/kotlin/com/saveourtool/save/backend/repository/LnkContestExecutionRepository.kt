package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * Repository of [LnkContestExecution]
 */
@Repository
interface LnkContestExecutionRepository : BaseEntityRepository<LnkContestExecution> {
    /**
     * Get N best scores of a [Project] in contest with name [contestName]
     *
     * @param project
     * @param contestName
     * @param pageable
     * @return record with a page of N best scores of a contests of the project
     */
    fun findByExecutionProjectAndContestNameOrderByScoreDesc(project: Project, contestName: String, pageable: Pageable): Page<LnkContestExecution>
}
