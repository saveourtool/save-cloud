package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.springframework.data.jpa.repository.JpaRepository

interface ExecutionRepository : JpaRepository<Execution, Long>