package org.cqfn.save.entities

import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 * @property role role of this user
 * @property source where the user identity is coming from, e.g. "github"
 * @property avatar avatar of user
 */
@Entity
class User(
    var name: String?,
    var password: String?,
    var role: String?,
    var source: String,
    var avatar: String? = null,
) : BaseEntity()
