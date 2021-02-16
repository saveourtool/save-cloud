package org.cqfn.save.backend.entities

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
data class Billionaires(val firstName: String, val lastName: String, val career: String) {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Int? = null
}
