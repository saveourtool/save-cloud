package com.saveourtool.save.entities

import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.validation.Validatable
import com.saveourtool.save.validation.isValidName

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * @property name organization
 * @property dateCreated date created organization
 * @property avatar
 * @property description
 * @property canCreateContests
 */
@Serializable
data class OrganizationDto(
    val name: String,
    @Contextual
    val dateCreated: LocalDateTime? = null,
    val avatar: String? = null,
    val description: String = "",
    val canCreateContests: Boolean = false,
) : Validatable {
    /**
     * Validation of organization name
     *
     * @return true if name is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun validateName(): Boolean = name.isValidName()

    /**
     * Validation of an organization
     *
     * @return true if organization is valid, false otherwise
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    override fun validate(): Boolean = validateName()

    companion object {
        /**
         * Value that represents an empty [OrganizationDto]
         */
        val empty = OrganizationDto(
            name = "",
            dateCreated = null,
            avatar = null,
            description = "",
            canCreateContests = false,
        )
    }
}
