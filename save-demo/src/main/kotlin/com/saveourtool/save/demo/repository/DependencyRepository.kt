package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [Dependency] entity.
 */
@Repository
interface DependencyRepository : BaseEntityRepository<Dependency> {
    /**
     * @param coreTool
     * @return list of [coreTool]'s [Dependency]s
     */
    fun findByCoreTool(coreTool: Tool): List<Dependency>
}
