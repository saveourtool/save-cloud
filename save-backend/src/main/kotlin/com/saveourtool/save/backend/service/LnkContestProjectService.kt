package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkContestProjectRepository
import com.saveourtool.save.entities.*
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * Service of [LnkContestProject]
 */
@Service
class LnkContestProjectService(
    private val lnkContestProjectRepository: LnkContestProjectRepository,
) {
    /**
     * @param contestName name of a [Contest]
     * @return list of links contest-project for a contest
     */
    fun getAllByContestName(contestName: String): List<LnkContestProject> = lnkContestProjectRepository.findByContestName(contestName)

    fun isProjectRegisteredForContest(project: Project, contestName: String) = lnkContestProjectRepository.findByContestNameAndProject(contestName, project).isPresent

    /**
     * @param projectName
     * @param organizationName
     * @param numberOfContests
     * @return list of best [numberOfContests] contests of a project
     */
    fun getByProjectNameAndOrganizationName(
        projectName: String,
        organizationName: String,
        numberOfContests: Int,
    ): List<LnkContestProject> = lnkContestProjectRepository.findByProjectNameAndProjectOrganizationName(
        projectName,
        organizationName,
        PageRequest.ofSize(numberOfContests),
    )

    /**
     * @param contestName name of a contest
     * @param projects list of projects
     * @return list of [Project]s that are participating in contest with name [contestName] and are in list [projects]
     */
    fun getProjectsFromListAndContest(contestName: String, projects: List<Project>) = projects.mapNotNull { it.id }
        .toSet()
        .let {
            lnkContestProjectRepository.findByContestNameAndProjectIdIn(contestName, it)
        }

    /**
     * @param project a [Project]
     * @param contest a [Contest]
     * @return true if record is saved, false if is already present
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun saveLnkContestProject(project: Project, contest: Contest): Boolean = if (lnkContestProjectRepository.findByContestAndProject(contest, project).isPresent) {
        false
    } else {
        lnkContestProjectRepository.save(LnkContestProject(project, contest))
        true
    }
}
