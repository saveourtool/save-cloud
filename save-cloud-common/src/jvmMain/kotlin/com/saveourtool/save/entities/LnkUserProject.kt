package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
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
class LnkUserProject(
    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project?,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @Enumerated(EnumType.STRING)
    var role: Role?,
) : BaseEntity()
