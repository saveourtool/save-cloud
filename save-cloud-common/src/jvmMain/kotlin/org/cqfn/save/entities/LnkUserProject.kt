package org.cqfn.save.entities

import org.cqfn.save.domain.Role

import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy

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
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
