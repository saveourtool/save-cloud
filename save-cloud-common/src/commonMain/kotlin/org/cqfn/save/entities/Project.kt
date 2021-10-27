package org.cqfn.save.entities

import kotlinx.serialization.Serializable

/**
 * @property owner
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 */
@Entity
@Serializable
data class Project(
    var owner: String,
    var name: String,
    var url: String?,
    var description: String?,
    var status: String?,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null
}
