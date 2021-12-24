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
    var user: User?,

    var adminIds: String? = "",
) : BaseEntity() {
    fun toDto() = ProjectDto(
        id = id!!,
        owner = owner,
        name = name,
        url = url,
        description = description,
        status = status,
        public = public,
        username = user!!.name!!,
    )

    companion object {
        fun stub(id: Long) = Project(
            owner = "stub",
            name = "stub",
            url = null,
            description = null,
            status = ProjectStatus.CREATED,
            user = null,
            adminIds = null,
        ).apply {
            this.id = id
        }

        fun fromDto(projectDto: ProjectDto) = with(projectDto) {
            Project(
                owner = owner,
                name = name,
                url = url,
                description = description,
                status = status,
                public = public,
                user = User(name = username, null, null, ""),
                adminIds = null,
            ).apply {
                id = id?.takeIf { it > 0 }
            }
        }
    }
}
