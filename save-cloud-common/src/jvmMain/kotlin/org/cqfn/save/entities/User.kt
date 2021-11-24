package org.cqfn.save.entities

import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 */
@Entity
class User(
    var name: String?,
    var password: String?,
    var role: String?,
) : BaseEntity()
