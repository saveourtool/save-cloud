package org.cqfn.save.entities

import java.time.LocalDateTime
import javax.persistence.Entity

/**
 * @property name organization
 * @property owner organization
 * @property dateCreated date created organization
 */
@Entity
class Organization(
    var name: String?,
    var owner: Int?,
    var dateCreated: LocalDateTime?,
) : BaseEntity()
