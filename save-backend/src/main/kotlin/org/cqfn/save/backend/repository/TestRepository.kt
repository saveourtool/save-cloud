package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository of tests
 */
@Repository
interface TestRepository : BaseEntityRepository<Test> {
    /**
     * Method to retrieve ready batches
     *
     * @param limit
     * @param offset
     * @return List of Tests
     */
    @Query(value = "select * from test inner join test_execution on test.id = test_execution.test_id and test_execution.status = 'READY' and test_execution.test_suite_execution_id = ?3 limit ?1 offset ?2", nativeQuery = true)
    fun retrieveBatches(limit: Int, offset: Int, executionId: Long): List<Test>
}
