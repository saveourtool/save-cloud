package com.saveourtool.save.entities

import javax.persistence.Entity
import javax.persistence.FetchType
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,
    var source: String?,
) : BaseEntity()
