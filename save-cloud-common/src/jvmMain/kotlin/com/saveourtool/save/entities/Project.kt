package com.saveourtool.save.entities

import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.utils.EnumType
import com.saveourtool.save.validation.isValidEmail

import javax.persistence.Entity
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

import kotlinx.serialization.Serializable

/**
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 * @property organization
 * @property email
 * @property numberOfContainers
 * @property contestRating global rating based on all contest results associated with this project
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
    var email: String? = null,
    var numberOfContainers: Int = 3,

    @ManyToOne
    @JoinColumn(name = "organization_id")
    var organization: Organization,
    var contestRating: Double = 0.0,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    /**
     * @return [id] as not null with validating
     * @throws IllegalArgumentException when [id] is not set that means entity is not saved yet
     */
    fun requiredId(): Long = requireNotNull(id) {
        "Entity is not saved yet: $this"
    }

    /**
     * Email validation
     *
     * @return true if email is valid, false otherwise
     */
    fun validateEmail() = email.isNullOrEmpty() || email?.isValidEmail() ?: true

    /**
     * @return [ProjectDto] from [Project]
     */
    fun toDto() = ProjectDto(
        name,
        organization.name,
        public,
        description ?: "",
        url ?: "",
        email ?: "",
    )

    /**
     * Return the shortest unique representation of this [Project] as a string
     */
    fun shortToString() = "[organization=${organization.name},name=$name]"

    /**
     * @return [ProjectCoordinates] is built for current entity
     */
    fun toProjectCoordinates(): ProjectCoordinates = ProjectCoordinates(
        organizationName = organization.name,
        projectName = name,
    )

    companion object {
        /**
         * Create a stub for testing. Since all fields are mutable, only required ones can be set after calling this method.
         *
         * @param id id of created project
         * @param organization
         * @return a project
         */
        fun stub(
            id: Long?,
            organization: Organization = Organization("stub", OrganizationStatus.CREATED, null, null, null)
        ) = Project(
            name = "stub",
            url = null,
            description = null,
            status = ProjectStatus.CREATED,
            organization = organization,
        ).apply {
            this.id = id
        }
    }
}

/**
 * @param organization organization that is an owner of a given project
 * @param status
 * @return [Project] from [ProjectDto]
 */
fun ProjectDto.toProject(
    organization: Organization,
    status: ProjectStatus = ProjectStatus.CREATED,
) = Project(
    name = name,
    url = url,
    description = description,
    status = status,
    public = isPublic,
    email = email,
    numberOfContainers = numberOfContainers,
    organization = organization,
    contestRating = contestRating,
)
