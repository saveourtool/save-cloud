package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestBatchDto
import org.cqfn.save.test.TestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService(private val configProperties: ConfigProperties) {
    private var offset = AtomicInteger(0)

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    /**
     * @param tests
     */
    fun saveTests(tests: List<TestDto>) {
        tests.map { testDto ->
            testSuiteRepository.findById(testDto.testSuiteId).ifPresent {
                testRepository.findByHash(testDto.hash).ifPresentOrElse(
                    {},
                    { testRepository.save(Test(testDto.hash, testDto.filePath, LocalDateTime.now(), it)) })
            }
        }
    }

    /**
     * @return Test batches
     */
    fun getTestBatches(): Mono<List<TestBatchDto>> {
        val tests = testRepository.retrieveBatches(configProperties.limit, offset.get()).map {
            TestBatchDto(it.filePath, it.testSuite.id!!, it.id!!)
        }
        offset.addAndGet(configProperties.limit)
        return Mono.just(tests)
    }
}
