package org.cqfn.save.backend.repository

import org.cqfn.save.entities.LnkUserProject
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository

/**
 * Repository of lnkUserProject
 */
@Repository
interface LnkUserProjectRepository : BaseEntityRepository<LnkUserProject> {
    /**
     * @param project
     * @return lnkUserProject by project
     */
    fun findByProject(project: Project): List<LnkUserProject>

    /**
     * @param userId
     * @param project
     * @return lnkUserProject by user ID and project
     */
    fun findByUserIdAndProject(userId: Long, project: Project): LnkUserProject?
}
