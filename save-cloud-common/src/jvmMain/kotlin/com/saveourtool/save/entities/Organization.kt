package com.saveourtool.save.entities

import com.saveourtool.save.spring.entity.BaseEntityWithDto
import java.time.LocalDateTime
import javax.persistence.*

import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property status
 * @property dateCreated date created organization
 * @property avatar
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
    var dateCreated: LocalDateTime,
    var avatar: String? = null,
    var description: String? = null,
    var canCreateContests: Boolean = false,
) : BaseEntityWithDto<OrganizationDto>() {
    /**
     * @return [OrganizationDto]
     */
    override fun toDto() = OrganizationDto(
        name = name,
        status = status,
        dateCreated = dateCreated.toKotlinLocalDateTime(),
        avatar = avatar,
        description = description.orEmpty(),
        canCreateContests = canCreateContests,
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
        ) = OrganizationDto.empty
            .copy(
                name = "stub"
            )
            .toOrganization()
            .apply {
                this.id = id
            }
    }
}

/**
 * @return [Organization] from [OrganizationDto]
 */
fun OrganizationDto.toOrganization() = Organization(
    name = name,
    status = status,
    dateCreated = dateCreated.toJavaLocalDateTime(),
    avatar = avatar,
    description = description,
    canCreateContests = canCreateContests,
)
