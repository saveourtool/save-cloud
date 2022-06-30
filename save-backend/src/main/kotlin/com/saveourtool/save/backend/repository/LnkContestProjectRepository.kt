package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.LnkContestProject
import com.saveourtool.save.entities.Project
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

/**
 * Repository of [LnkContestProject]
 */
@Repository
interface LnkContestProjectRepository : BaseEntityRepository<LnkContestProject> {
    /**
     * @param project
     * @return [LnkContestProject] by [project]
     */
    fun findByProject(project: Project): List<LnkContestProject>

    /**
     * @param contestId
     * @param projectId
     * @return [LnkContestProject] by [contestId] and [projectId]
     */
    fun findByContestIdAndProjectId(contestId: Long, projectId: Long): Optional<LnkContestProject>

    /**
     * Save [LnkContestProject] using only ids and role string.
     *
     * @param contestId
     * @param projectId
     * @param score
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_contest_project (project_id, contest_id, score) values (:project_id, :contest_id, :score)",
        nativeQuery = true,
    )
    fun save(
        @Param("project_id") projectId: Long,
        @Param("contest_id") contestId: Long,
        @Param("score") score: Float
    )
}
