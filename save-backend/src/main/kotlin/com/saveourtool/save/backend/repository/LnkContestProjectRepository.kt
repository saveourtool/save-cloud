package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Contest
import com.saveourtool.save.entities.LnkContestProject
import com.saveourtool.save.entities.Project
import org.springframework.data.domain.Pageable
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
     * @param contest
     * @param project
     * @return [LnkContestProject] by [contest] and [project]
     */
    fun findByContestAndProject(contest: Contest, project: Project): Optional<LnkContestProject>

    /**
     * @param projectName
     * @param organizationName
     * @param pageable
     * @return link contest-project for project with name [projectName] and contest with name [contestName]
     */
    fun findByProjectNameAndProjectOrganizationName(
        projectName: String,
        organizationName: String,
        pageable: Pageable,
    ): List<LnkContestProject>

    /**
     * @param contestName
     * @return list of [LnkContestProject] linked to contest with name [contestName]
     */
    fun findByContestName(contestName: String): List<LnkContestProject>

    /**
     * Save [LnkContestProject] using only ids and contest score.
     *
     * @param contestId
     * @param projectId
     * @return saved [LnkContestProject] record
     */
    @Transactional
    @Modifying
    @Query(
        value = "insert into save_cloud.lnk_contest_project (project_id, contest_id) values (:project_id, :contest_id)",
        nativeQuery = true,
    )
    fun save(
        @Param("project_id") projectId: Long,
        @Param("contest_id") contestId: Long,
    ): LnkContestProject
}
