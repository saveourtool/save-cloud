package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * Repository of execution
 */
@Repository
interface ExecutionRepository : BaseEntityRepository<Execution> {
    /**
     * @param name name of project
     * @param organization organization of project
     * @return list of executions
     */
    fun getAllByProjectNameAndProjectOrganization(name: String, organization: Organization): List<Execution>

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name name of project
     * @param organization organization of project
     * @return execution or null if it was not found
     */
    fun findTopByProjectNameAndProjectOrganizationOrderByStartTimeDesc(name: String, organization: Organization): Optional<Execution>

    /**
     * @param project to find execution
     * @return execution
     */
    fun findTopByProjectOrderByStartTimeDesc(project: Project): Execution?
}
