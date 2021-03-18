package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Project
import org.springframework.data.jpa.repository.JpaRepository

/**
 * The repository of project entities
 */
interface ProjectRepository : JpaRepository<Project, Long>
