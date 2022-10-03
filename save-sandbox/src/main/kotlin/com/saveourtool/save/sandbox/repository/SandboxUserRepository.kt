package com.saveourtool.save.sandbox.repository

import com.saveourtool.save.sandbox.entity.SandboxUser
import com.saveourtool.save.spring.repository.BaseEntityRepository
import org.springframework.stereotype.Repository

@Repository
interface SandboxUserRepository : BaseEntityRepository<SandboxUser> {
    /**
     * @param name
     * @return [SandboxUser] found by [name]
     */
    fun findByName(name: String): SandboxUser?
}