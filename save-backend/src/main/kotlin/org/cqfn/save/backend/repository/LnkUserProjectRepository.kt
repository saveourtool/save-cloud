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

    fun findByUserIdAndProject(userId: Long, project: Project): List<LnkUserProject>
}
