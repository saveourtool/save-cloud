package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Execution
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository of execution
 */
@Repository
interface ExecutionRepository : BaseEntityRepository<Execution> {
    @Modifying
    @Query("update execution set execution.offset = ?1 where execution.id = ?2", nativeQuery = true)
    fun setNewOffset(offset: Int, id: Long)
}
