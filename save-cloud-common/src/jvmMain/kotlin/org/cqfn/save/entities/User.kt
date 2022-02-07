package org.cqfn.save.entities

import org.cqfn.save.domain.Role
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property source where the user identity is coming from, e.g. "github"
 */
@Entity
class User(
    var name: String?,
    var password: String?,
    @Enumerated(EnumType.STRING)
    var role: Role?,
    var source: String,
) : BaseEntity()
