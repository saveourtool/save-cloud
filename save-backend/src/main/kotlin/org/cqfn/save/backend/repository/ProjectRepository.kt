package org.cqfn.save.backend.repository

import org.cqfn.save.backend.entities.Project
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.JpaRepository

@Profile("dev")
interface ProjectRepository : JpaRepository<Project, Int>
