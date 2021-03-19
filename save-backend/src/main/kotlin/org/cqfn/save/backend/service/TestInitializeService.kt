package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestInitializeService {
    @Autowired
    private lateinit var testRepository: TestRepository

    /**
     * @param tests
     */
    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }
}
