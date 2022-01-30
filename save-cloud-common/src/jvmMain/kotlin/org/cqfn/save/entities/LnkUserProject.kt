package org.cqfn.save.entities

import org.cqfn.save.user.UserRole
import javax.persistence.Entity

/**
 * @property project
 * @property user in project
 * @property role role of this user
 */
@Entity
class LnkUserProject(
    var project: Project?,
    var user: User?,
    var role: UserRole?,
) : BaseEntity()
