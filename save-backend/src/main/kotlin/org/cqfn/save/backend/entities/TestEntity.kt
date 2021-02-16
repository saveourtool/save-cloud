package org.cqfn.save.backend.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class TestEntity(val value: String) {
    constructor() : this("default")
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null
}