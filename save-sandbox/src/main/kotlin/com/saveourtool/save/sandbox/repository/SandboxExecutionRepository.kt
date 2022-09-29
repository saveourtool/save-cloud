package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.sandbox.entity.SandboxExecution
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

@Repository
interface SandboxExecutionRepository : BaseEntityRepository<SandboxExecution>
