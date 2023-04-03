package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.JoinColumn

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
    @Column(name = "owner")
    var githubOwner: String,
    @Column(name = "repo_name")
    var githubRepoName: String,
) : BaseEntity()
