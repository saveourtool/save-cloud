package com.saveourtool.save.demo.entity

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @property organizationName
 * @property projectName
 */
@Entity
@Table(name = "git_repo")
class GithubRepo(
    var organizationName: String,
    var projectName: String,
) : BaseEntity() {
    /**
     * @return pretty string that defines [GithubRepo]
     */
    fun toPrettyString() = "$organizationName/$projectName"
}

/**
 * @return [GithubRepo] from [ProjectCoordinates]
 */
fun ProjectCoordinates.toGithubRepo() = GithubRepo(
    organizationName,
    projectName,
)
