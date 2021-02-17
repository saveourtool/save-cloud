package org.cqfn.save.backend.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class Project(
    var owner: String,
    var name: String,
    var url: String,
    @Id @GeneratedValue(strategy = GenerationType.AUTO) var id: Int? = null
)
