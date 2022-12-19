package com.saveourtool.save.demo.entity

import com.saveourtool.save.demo.storage.ToolKey
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.*

/**
 * @property gitRepo
 * @property snapshot
 */
@Entity
@Table(name = "tool")
class Tool(
    @ManyToOne
    @JoinColumn(name = "git_repo_id")
    var gitRepo: GitRepo,
    @ManyToOne
    @JoinColumn(name = "snapshot_id")
    var snapshot: Snapshot,
) : BaseEntity() {
    /**
     * @return [ToolKey] from [Tool] entity
     */
    fun toToolKey() = ToolKey(
        gitRepo.organizationName,
        gitRepo.toolName,
        snapshot.version,
        snapshot.executableName,
    )
}
