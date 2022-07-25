package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import com.saveourtool.save.info.OrganizationInfo
import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.utils.LocalDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property ownerId organization
 * @property dateCreated date created organization
 * @property avatar
 * @property status
 * @property description
 * @property canCreateContests
 */
@Entity
@Serializable
data class Organization(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: OrganizationStatus,
    var ownerId: Long? = null,
    @Contextual
    var dateCreated: LocalDateTime?,
    var avatar: String? = null,
    var description: String? = null,
    var canCreateContests: Boolean = false,
) {
    /**
     * id of organization
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    /**
     * @param userRoles map where keys are usernames and values are their roles
     * @return [OrganizationInfo]
     */
    fun toOrganizationInfo(userRoles: Map<String, Role> = emptyMap()) = OrganizationInfo(
        name,
        userRoles,
        avatar
    )

    companion object {
        /**
         * Create a stub for testing.
         *
         * @param id id of created organization
         * @return an organization
         */
        fun stub(
            id: Long?,
        ) = Organization(
            name = "stub",
            status = OrganizationStatus.CREATED,
            ownerId = -1,
            dateCreated = null,
            avatar = null,
            description = null,
            canCreateContests = false,
        ).apply {
            this.id = id
        }
    }
}
