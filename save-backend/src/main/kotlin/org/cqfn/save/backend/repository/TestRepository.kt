package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.stereotype.Repository
import java.util.*

/**
 * Repository of tests
 */
@Repository
interface TestRepository : BaseEntityRepository<Test> {
    fun findByHash(hash: String): Test?
}
