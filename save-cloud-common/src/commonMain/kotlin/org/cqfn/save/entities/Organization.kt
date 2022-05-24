package com.saveourtool.save.entities

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
 */
@Entity
@Serializable
@Suppress("USE_DATA_CLASS")
data class Organization(
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: OrganizationStatus,
    var ownerId: Long? = null,
    @Contextual
    var dateCreated: LocalDateTime?,
    var avatar: String? = null,
    var description: String? = null,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

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
        ).apply {
            this.id = id
        }
    }
}
