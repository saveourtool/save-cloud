package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Git
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository

/**
 * Repository of git
 */
@Repository
interface GitRepository : BaseEntityRepository<Git> {
    /**
     * @param project
     * @return git by project
     */
    fun findByProject(project: Project): Git?
}
