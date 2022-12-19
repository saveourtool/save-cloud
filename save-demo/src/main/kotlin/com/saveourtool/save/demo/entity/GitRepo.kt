package com.saveourtool.save.demo.entity

import com.saveourtool.save.spring.entity.BaseEntity
import javax.persistence.Entity
import javax.persistence.Table

/**
 * @property organizationName
 * @property toolName
 */
@Entity
@Table(name = "git_repo")
class GitRepo(
    var organizationName: String,
    var toolName: String,
) : BaseEntity()
