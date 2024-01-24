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
 * @property user in project
 * @property role role of this user
 */
@Entity
@Table(schema = "save_cloud", name = "lnk_user_project")
class LnkUserProject(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @Enumerated(EnumType.STRING)
    var role: Role,
) : BaseEntity()
