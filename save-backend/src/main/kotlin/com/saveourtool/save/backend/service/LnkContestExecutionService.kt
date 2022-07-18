package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkContestExecutionRepository
import com.saveourtool.save.entities.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * Service of [LnkContestExecution]
 */
@Service
class LnkContestExecutionService(
    private val lnkContestExecutionRepository: LnkContestExecutionRepository,
) {
    /**
     * @param project
     * @param contestName
     * @return best score of a [Project] in contest with name [contestName]
     */
    fun getBestScoreOfProjectInContestWithName(project: Project, contestName: String) =
            lnkContestExecutionRepository.findByExecutionProjectAndContestNameOrderByScoreDesc(project, contestName, Pageable.ofSize(1))
                .content
                .singleOrNull()
                ?.score
}
