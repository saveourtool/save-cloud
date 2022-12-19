package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.GitRepo
import com.saveourtool.save.demo.entity.Snapshot
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [Tool] entity.
 */
@Repository
interface ToolRepository : BaseEntityRepository<Tool> {
    /**
     * @param gitRepo
     * @return list of [Tool]s corresponding to [gitRepo]
     */
    fun findByGitRepo(gitRepo: GitRepo): List<Tool>

    /**
     * @param gitRepo
     * @param snapshot
     * @return [Tool] from [gitRepo] repository corresponding [snapshot].
     */
    fun findByGitRepoAndSnapshot(gitRepo: GitRepo, snapshot: Snapshot): Tool?

    /**
     * @param gitRepo
     * @param versionTag version control system tag name
     * @return list of [Tool]s from [gitRepo] repository that match [versionTag] version
     */
    fun findByGitRepoAndSnapshotVersion(
        gitRepo: GitRepo,
        versionTag: String,
    ): List<Tool>
}
