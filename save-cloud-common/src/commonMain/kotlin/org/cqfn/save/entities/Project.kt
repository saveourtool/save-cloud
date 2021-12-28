package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

import kotlinx.serialization.Serializable

/**
 * @property owner
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 * @property userId the user that has created this project
 * @property adminIds comma-separated list of IDs of users that are admins of this project
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
    var public: Boolean = true,
    @ManyToOne
    @JoinColumn(
        name = "user_id",
        columnDefinition = "",
        referencedColumnName = "",
        unique = false,
        nullable = false,
        insertable = false,
        updatable = false,
        table = "",
        foreignKey = ForeignKey()
    )
    var userId: Long,
    var adminIds: String?,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    companion object {
        fun stub(id: Long?) = Project(
            name = "stub",
            owner = "stub",
            url = null,
            description = null,
            status = ProjectStatus.CREATED,
            userId = -1,
            adminIds = null,
        ).apply {
            this.id = id
        }
    }
}
