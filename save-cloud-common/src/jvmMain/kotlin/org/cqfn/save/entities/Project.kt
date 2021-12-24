package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * @property owner
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 * @property user the user that has created this project
 * @property adminIds comma-separated list of IDs of users that are admins of this project
 */
@Entity
data class Project(
    var owner: String,
    var name: String,
    var url: String?,
    var description: String?,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus,
    var public: Boolean = true,

    @ManyToOne
    @JoinColumn(name = "user_id")
    var user: User,

    var adminIds: String,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    fun toDto() = ProjectDto(
        owner = owner,
        name = name,
        url = url,
        description = description,
        status = status,
        public = public,
    )
}
