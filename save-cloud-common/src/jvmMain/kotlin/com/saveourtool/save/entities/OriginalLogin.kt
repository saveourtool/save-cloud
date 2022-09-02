package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property name
 * @property user
 * @property source
 */
@Entity
class OriginalLogin(
    var name: String?,
    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,
    var source: String?,
) : BaseEntity()
