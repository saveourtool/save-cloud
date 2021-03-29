package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService(private val configProperties: ConfigProperties) {
    @Autowired
    private lateinit var testRepository: TestRepository

    private var offset = AtomicInteger(0)

    /**
     * @param tests
     */
    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }

    /**
     * @return Test batches
     */
    fun getTestBatches(): Mono<List<TestDto>> {
        val tests = testRepository.retrieveBatches(configProperties.limit, offset.get()).map {
            TestDto(it.expectedFilePath, it.testFilePath, it.testSuiteId, it.id)
        }
        offset.addAndGet(configProperties.offset)
        return Mono.just(tests)
    }
}
