package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkContestProjectRepository
import com.saveourtool.save.entities.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.awt.print.Pageable
import java.util.Optional

/**
 * Service of [LnkContestProject]
 */
@Service
class LnkContestProjectService(
    private val lnkContestProjectRepository: LnkContestProjectRepository,
    private val lnkUserProjectService: LnkUserProjectService,
    private val projectService: ProjectService,
) {
    /**
     * @param projectId id of a [Project]
     * @return all contests that project with [projectId] participated
     */
    fun getAllContestsByProjectId(projectId: Long) = lnkContestProjectRepository.findByProjectId(projectId)

    /**
     * @param userId id of a [User]
     * @return list of links contest-project of projects where user with [userId] is a member
     */
    fun getAllContestsByProjectsWithUserId(userId: Long) = lnkUserProjectService.getAllProjectsByUserId(userId)
        .mapNotNull { it.id }
        .toSet()
        .let {
            lnkContestProjectRepository.findByProjectIdIn(it)
        }

    /**
     * @param contestName name of a [Contest]
     * @param projectName name of a [Project]
     * @return link between contest with [contestName] and project with [projectName]
     */
    fun getByContestNameAndProjectName(contestName: String, projectName: String): Optional<LnkContestProject> =
            lnkContestProjectRepository.findByContestNameAndProjectName(contestName, projectName)

    /**
     * @param contestName name of a [Contest]
     * @return list of links contest-project for a contest
     */
    fun getByContestName(contestName: String): List<LnkContestProject> = lnkContestProjectRepository.findByContestName(contestName)

    fun getBestContestsByProject(
        projectName: String,
        organizationName: String,
        numberOfContests: Int,
    ): List<LnkContestProject> = projectService.findByNameAndOrganizationName(projectName, organizationName)
        ?.let {
            lnkContestProjectRepository.findByProjectOrderByScoreDesc(it, PageRequest.ofSize(numberOfContests)).content
        } ?: emptyList()
}
