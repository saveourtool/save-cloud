package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ProjectRepository
import org.springframework.stereotype.Service

/**
 * Service for project
 *
 * @property projectRepository
 */
@Service
class ProjectService(private val projectRepository: ProjectRepository)
