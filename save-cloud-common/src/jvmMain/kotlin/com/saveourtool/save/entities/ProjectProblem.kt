package com.saveourtool.save.entities

import com.saveourtool.save.entities.vulnerabilities.Vulnerability
import com.saveourtool.save.spring.entity.BaseEntityWithDateAndDto
import javax.persistence.*

/**
 * @property name
 * @property description
 * @property critical
 * @property vulnerability
 * @property project
 * @property userId
 * @property isClosed
 */
@Entity
@Suppress("LongParameterList")
class ProjectProblem(

    var name: String,

    var description: String,

    @Enumerated(EnumType.STRING)
    var critical: ProjectProblemCritical,

    @ManyToOne
    @JoinColumn(name = "vulnerability_id")
    var vulnerability: Vulnerability?,

    @ManyToOne
    @JoinColumn(name = "project_id")
    var project: Project,

    var userId: Long,

    var isClosed: Boolean,

) : BaseEntityWithDateAndDto<ProjectProblemDto>() {
    override fun toDto() = ProjectProblemDto(
        name = name,
        description = description,
        critical = critical,
        identifier = vulnerability?.identifier,
        organizationName = project.organization.name,
        projectName = project.name,
        isClosed = isClosed,
        id = id,
    )
}
