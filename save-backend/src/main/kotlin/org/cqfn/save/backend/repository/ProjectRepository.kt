package org.cqfn.save.backend.repository

import org.cqfn.save.backend.entities.Project
import org.springframework.data.jpa.repository.JpaRepository

interface ProjectRepository : JpaRepository<Project, Int>
