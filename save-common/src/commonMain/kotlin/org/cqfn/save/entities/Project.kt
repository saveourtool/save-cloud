package org.cqfn.save.entities

/**
 * @property owner
 * @property name
 * @property type type of the project, e.g. github or manually registered
 * @property url
 * @property description description of the project, may be absent
 * @property id
 */
@Entity
class Project(
    var owner: String,
    var name: String,
    var type: String,
    var url: String,
    var description: String?,
    @Id @GeneratedValue var id: Long? = null,
)
