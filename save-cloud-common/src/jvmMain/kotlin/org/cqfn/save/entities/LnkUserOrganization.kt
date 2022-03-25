package org.cqfn.save.entities

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property organization
 * @property user in organization
 */
@Entity
class LnkUserOrganization(
    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization?,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

) : BaseEntity()
