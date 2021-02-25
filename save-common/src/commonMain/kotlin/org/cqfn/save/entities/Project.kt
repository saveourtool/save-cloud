package org.cqfn.save.entities

/**
 * @property owner
 * @property name
 * @property url
 * @property id
 */
@Entity
class Project(
    var owner: String,
    var name: String,
    var url: String,
    @Id @GeneratedValue var id: Int? = null
)
