package com.saveourtool.save.demo.entity

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @property organizationName
 * @property toolName
 */
@Entity
@Table(name = "git_repo")
class GithubRepo(
    var organizationName: String,
    @Column(name = "project_name")
    var toolName: String,
) : BaseEntity()
