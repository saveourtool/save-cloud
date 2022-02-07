package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.serialization.Serializable

/**
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 * @property userId the user that has created this project. No automatic mapping, because Hibernate is not available in common code.
 * @property organization
 */
@Entity
@Serializable
data class Project(
    var name: String,
    var url: String?,
    var description: String?,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus,
    var public: Boolean = true,
    var userId: Long? = null,
    var organization: Organization,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    companion object {
        private val date = LocalDateTime(1970, Month.JANUARY, 1, 0, 0, 1)

        /**
         * Create a stub for testing. Since all fields are mutable, only required ones can be set after calling this method.
         *
         * @param id id of created project
         * @return a project
         */
        fun stub(id: Long?) = Project(
            name = "stub",
            url = null,
            description = null,
            status = ProjectStatus.CREATED,
            userId = -1,
            organization = Organization("stub", null, date),
        ).apply {
            this.id = id
        }
    }
}
