package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Git
import org.cqfn.save.entities.Project
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.QueryByExampleExecutor
import org.springframework.stereotype.Repository

@Repository
interface GitRepository : JpaRepository<Git, Long>, QueryByExampleExecutor<Git> {
    fun findByProject(project: Project): Git
}
