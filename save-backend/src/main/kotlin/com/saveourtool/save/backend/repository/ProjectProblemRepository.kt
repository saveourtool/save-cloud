package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.ProjectProblem
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * Repository to access data about project problems
 */
@Repository
interface ProjectProblemRepository : BaseEntityRepository<ProjectProblem> {
    /**
     * @param projectName name of project
     * @param organizationName name of organization
     * @return list of project problems
     */
    fun getAllProblemsByProjectNameAndProjectOrganizationName(projectName: String, organizationName: String): List<ProjectProblem>
}
