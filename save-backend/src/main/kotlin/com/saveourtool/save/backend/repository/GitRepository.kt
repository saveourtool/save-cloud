package com.saveourtool.save.backend.repository

import com.saveourtool.save.entities.Git
import com.saveourtool.save.entities.Project
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
