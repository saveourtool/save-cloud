package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property project
 * @property githubOwner user/organization name from GitHub
 * @property githubRepoName GitHub repository name
 */
@Entity
class LnkProjectGithub(
    @OneToOne
    @JoinColumn(name = "project_id")
    var project: Project,
    @JoinColumn(name = "owner")
    var githubOwner: String,
    @JoinColumn(name = "repo_name")
    var githubRepoName: String,
) : BaseEntity()
