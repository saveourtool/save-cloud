package org.cqfn.save.entities

import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * base class for all entities
 */
@Suppress("USE_DATA_CLASS")
@MappedSuperclass
open class BaseEntity {
    /**
     * generate a unique id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    open var id: Long? = null
}
