package com.saveourtool.save.entities

import com.saveourtool.save.domain.Role
import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.utils.LocalDateTime

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * @property name organization
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
     * @return [OrganizationDto]
     */
    fun toDto(userRoles: Map<String, Role> = emptyMap()) = OrganizationDto(
        name,
        userRoles = userRoles,
        avatar = avatar,
    )
    
    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }

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
            dateCreated = null,
            avatar = null,
            description = null,
            canCreateContests = false,
        ).apply {
            this.id = id
        }
    }
}

/**
 * @param dateCreated date when organization was created
 * @param canCreateContests flag that defines whether an organization can create contests or not
 * @param status
 * @return [Organization] from [OrganizationDto]
 */
fun OrganizationDto.toOrganization(
    dateCreated: LocalDateTime?,
    canCreateContests: Boolean = false,
    status: OrganizationStatus = OrganizationStatus.CREATED,
) = Organization(
    name,
    status,
    dateCreated,
    avatar,
    description,
    canCreateContests,
)
