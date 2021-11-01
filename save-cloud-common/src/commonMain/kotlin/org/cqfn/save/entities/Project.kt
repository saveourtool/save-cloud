package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

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
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null
}
