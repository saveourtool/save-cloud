package org.cqfn.save.backend.controller

import org.cqfn.save.backend.SaveApplication
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.utils.MySqlExtension
import org.cqfn.save.entities.Execution
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [SaveApplication::class])
@AutoConfigureWebTestClient
@ExtendWith(MySqlExtension::class)
class BaseEntityRepositoryTest {
    @Autowired
    lateinit var executionRepository: ExecutionRepository

    @Test
    fun testFindList() {
        val executionToId = executionRepository.findById(1).get()

        val executionToList = executionRepository.getList(
            Execution::class
        ) { root, query, cb -> cb.equal(root.get<Long>("id"), 1) }[0]

        Assertions.assertTrue(executionToId.id == executionToList.id)
    }
}
