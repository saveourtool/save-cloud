package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService(private val configProperties: ConfigProperties) {
    private val log = LoggerFactory.getLogger(TestService::class.java)
    // May be better to keep offset in database in Execution
    private var offset = mutableMapOf<Long, AtomicInteger>()

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    /**
     * @param tests
     */
    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }

    /**
     * @return Test batches
     */
    @Transactional
    fun getTestBatches(agentId: String): Mono<List<TestDto>> {
        val agent = agentRepository.findByContainerId(agentId) ?: error("The specified agent does not exist")
        log.info("Agent found: $agent")
        val executionId = agent.execution.id
        if (!offset.contains(executionId)) {
            offset[executionId!!] = AtomicInteger(0)
        }
        log.info("Retrieving tests")
        val tests = testRepository.retrieveBatches(configProperties.limit, offset[executionId]!!.get()).map {
            TestDto(it.filePath, it.testSuite.id!!, it.id!!)
        }
        offset[executionId]!!.addAndGet(configProperties.limit)
        log.info("Increasing offset of execution - ${agent.execution}")
        log.info(offset.keys.toString())
        return Mono.just(tests)
    }
}
