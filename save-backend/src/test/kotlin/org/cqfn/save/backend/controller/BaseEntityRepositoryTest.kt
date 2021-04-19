package org.cqfn.save.backend.controller

import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ExtendWith(MySqlExtension::class)
class BaseEntityRepositoryTest {
    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Test
    fun testFindList() {
        val executionToId = executionRepository.findById(1).get()

        val executionToList = executionRepository.getList(
            Execution::class
        ) { root, query, cb ->
            cb.and(
                cb.equal(root.get<Long>("id"), 1),
                cb.equal(root.get<ExecutionStatus>("status"), ExecutionStatus.FINISHED)
            )
        }[0]

        Assertions.assertTrue(executionToId.id == executionToList.id)
    }
}
