package org.cqfn.save.entities

@Entity
class Project(
    var owner: String,
    var name: String,
    var url: String,
    @Id @GeneratedValue var id: Int? = null
)
