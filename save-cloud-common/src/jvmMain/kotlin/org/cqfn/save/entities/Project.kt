package org.cqfn.save.entities

import org.cqfn.save.utils.EnumType

import org.cqfn.save.mappers.ProjectMapper
import org.mapstruct.factory.Mappers
import javax.persistence.Entity

/**
 * @property owner
 * @property name
 * @property url
 * @property description description of the project, may be absent
 * @property status status of project
 * @property public
 */
@Entity
data class Project(
    var owner: String,
    var name: String,
    var url: String?,
    var description: String?,
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus,
    var public: Boolean = true,

    var user: User,
) {
    /**
     * id of project
     */
    @Id
    @GeneratedValue
    var id: Long? = null

    fun toDto() = mapper.toDto(this)

    companion object {
        val mapper: ProjectMapper = Mappers.getMapper(ProjectMapper::class.java)
    }
}
