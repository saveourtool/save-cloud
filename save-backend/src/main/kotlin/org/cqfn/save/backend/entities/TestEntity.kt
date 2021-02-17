package org.cqfn.save.backend.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class TestEntity(
    var value: String,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Long? = null
)