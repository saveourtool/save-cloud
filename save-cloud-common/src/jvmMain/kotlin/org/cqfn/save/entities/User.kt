package org.cqfn.save.entities

import javax.persistence.Entity

/**
 * @property name
 * @property password *in plain text*
 */
@Entity
class User(
    var name: String?,
    var password: String?
) : BaseEntity()
