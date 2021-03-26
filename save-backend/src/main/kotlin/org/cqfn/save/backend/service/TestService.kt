package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService {
    @Autowired
    private lateinit var testRepository: TestRepository

    /**
     * @param tests
     */
    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }

    /**
     * @return Test batches
     */
    // Fixme: Stub in here
    fun getTestBatches() = Mono.just(listOf(TestDto("qwe", "www", 0, "id"),
        TestDto("qwe", "www", 0, "qweqwe")))
}
