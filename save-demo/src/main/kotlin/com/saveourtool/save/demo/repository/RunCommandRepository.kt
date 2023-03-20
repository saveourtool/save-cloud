package com.saveourtool.save.demo.repository

import com.saveourtool.save.demo.entity.RunCommand
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

/**
 * JPA repository for [RunCommand] entity.
 */
@Repository
interface RunCommandRepository : BaseEntityRepository<RunCommand>
