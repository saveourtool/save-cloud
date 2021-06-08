package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Git
import org.cqfn.save.entities.Project
import org.springframework.stereotype.Repository

@Repository
interface GitRepository : BaseEntityRepository<Git> {
    fun findByProject(project: Project): Git
}
