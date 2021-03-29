package org.cqfn.save.backend.repository

import org.cqfn.save.entities.Test
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * Repository of tests
 */
interface TestRepository : JpaRepository<Test, String> {
    /**
     * Method to retrieve ready batches
     *
     * @param limit
     * @param offset
     * @return List of Tests
     */
    @Query(value = "select * from test inner join test_execution on test.id = test_execution.test_id and test_execution.status = 'READY' limit ?1 offset ?2", nativeQuery = true)
    fun retrieveBatches(limit: Int, offset: Int): List<Test>
}
