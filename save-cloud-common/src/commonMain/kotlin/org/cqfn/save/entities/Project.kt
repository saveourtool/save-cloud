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
 * @property userId the user that has created this project. No automatic mapping, because Hibernate is not available in common code.
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
    var userId: Long? = null,
    var adminIds: String? = null,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    fun adminIdList() = adminIds?.split(",")?.map { it.toLong() } ?: emptyList()

    companion object {
        /**
         * Create a stub for testing. Since all fields are mutable, only required ones can be set after calling this method.
         *
         * @param id id of created project
         * @return a project
         */
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
