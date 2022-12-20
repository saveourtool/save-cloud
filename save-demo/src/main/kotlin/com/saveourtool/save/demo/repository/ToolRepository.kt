package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.GithubRepo
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
     * @param githubRepo
     * @return list of [Tool]s corresponding to [githubRepo]
     */
    fun findByGithubRepo(githubRepo: GithubRepo): List<Tool>

    /**
     * @param githubRepo
     * @param snapshot
     * @return [Tool] from [githubRepo] repository corresponding [snapshot].
     */
    fun findByGithubRepoAndSnapshot(githubRepo: GithubRepo, snapshot: Snapshot): Tool?

    /**
     * @param githubRepo
     * @param versionTag version control system tag name
     * @return list of [Tool]s from [githubRepo] repository that match [versionTag] version
     */
    fun findByGithubRepoAndSnapshotVersion(
        githubRepo: GithubRepo,
        versionTag: String,
    ): List<Tool>
}
