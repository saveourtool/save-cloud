package org.cqfn.save.mappers

import org.cqfn.save.entities.Project
import org.cqfn.save.entities.ProjectDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface ProjectMapper {
    // for properties with default value explicit mapping is required
    @Mapping(source = "public", target = "public")
    fun toDto(project: Project): ProjectDto
}
