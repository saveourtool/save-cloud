package org.cqfn.save.entities

import javax.persistence.Entity

@Entity
class User(
    var name: String?,
    var password: String?
) : BaseEntity()
