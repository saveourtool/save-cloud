package com.saveourtool.save.demo.entity

import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.*

/**
 * @property githubRepo
 * @property snapshot
 */
@Entity
class Tool(
    @ManyToOne
    @JoinColumn(name = "git_repo_id")
    var githubRepo: GithubRepo,
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    var snapshot: Snapshot,
) : BaseEntity() {
    /**
     * @return [ToolKey] from [Tool] entity
     */
    fun toToolKey() = ToolKey(
        githubRepo.organizationName,
        githubRepo.projectName,
        snapshot.version,
        snapshot.executableName,
    )
}
