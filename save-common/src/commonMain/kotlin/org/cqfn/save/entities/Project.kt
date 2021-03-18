package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property owner
 * @property name
 * @property type type of the project, e.g. github or manually registered
 * @property url
 * @property description description of the project, may be absent
 */
@Entity
@Serializable
data class Project(
    var owner: String,
    var name: String,
    var type: String,
    var url: String,
    var description: String?,
) {
    @Id @GeneratedValue var id: Long? = null
}
