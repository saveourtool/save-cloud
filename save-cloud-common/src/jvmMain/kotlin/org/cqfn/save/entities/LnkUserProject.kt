package org.cqfn.save.entities

import org.cqfn.save.domain.Role
import javax.persistence.Entity
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
    var user: User?,

    var role: Role?,
) : BaseEntity()
