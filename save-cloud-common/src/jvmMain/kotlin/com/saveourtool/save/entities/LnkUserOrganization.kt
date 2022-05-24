package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import javax.persistence.*
import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property organization
 * @property user in [organization]
 * @property role of [user] in [organization]
 */
@Entity
class LnkUserOrganization(
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization?,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    @Enumerated(EnumType.STRING)
    var role: Role?,
) : BaseEntity()
