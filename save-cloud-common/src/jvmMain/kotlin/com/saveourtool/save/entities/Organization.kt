package com.saveourtool.save.entities

import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.utils.LocalDateTime

import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

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
     * @return [OrganizationDto]
     */
    fun toDto() = OrganizationDto(
        name = name,
        dateCreated = dateCreated,
        avatar = avatar,
        description = description.orEmpty(),
        canCreateContests = canCreateContests,
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
 * @param status
 * @return [Organization] from [OrganizationDto]
 */
fun OrganizationDto.toOrganization(
    status: OrganizationStatus = OrganizationStatus.CREATED,
) = Organization(
    name,
    status,
    dateCreated,
    avatar,
    description,
    canCreateContests,
)
