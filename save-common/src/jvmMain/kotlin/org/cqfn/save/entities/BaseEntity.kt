package org.cqfn.save.entities

import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

/**
 * base class for all entities
 */
@MappedSuperclass
abstract class BaseEntity {
    /**
     * generate a unique id
     */
    @Id
    @GeneratedValue
    open var id: Long? = null
}
