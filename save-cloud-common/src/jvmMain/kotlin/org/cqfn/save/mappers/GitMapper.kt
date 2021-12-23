package org.cqfn.save.mappers

import org.cqfn.save.entities.Git
import org.cqfn.save.entities.GitDto
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface GitMapper {
    @Mapping(target = "hash", ignore = true)
    fun toDto(git: Git): GitDto
}
