package com.saveourtool.save.demo.service

import com.saveourtool.save.demo.entity.Dependency
import com.saveourtool.save.demo.entity.Tool
import com.saveourtool.save.demo.repository.DependencyRepository
import org.springframework.stereotype.Service

/**
 * [Service] for [Dependency] entity
 */
@Service
class DependencyService(
    private val dependencyRepository: DependencyRepository,
) {
    /**
     * @param coreTool
     * @param dependencyTool
     * @return [Dependency] entity that has been saved to database
     */
    fun addDependency(coreTool: Tool, dependencyTool: Tool): Dependency = dependencyRepository.findByCoreToolAndDependencyTool(
        coreTool,
        dependencyTool,
    ) ?: dependencyRepository.save(Dependency(coreTool, dependencyTool))
}
