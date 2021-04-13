package org.cqfn.save.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * base class for all entities
 */
@Suppress("USE_DATA_CLASS")
@Entity
class BaseEntity {
    /**
     * generate a unique id
     */
    @Id @GeneratedValue var id: Long? = null
}
