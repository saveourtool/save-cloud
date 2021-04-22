package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Execution
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
    // This field should be a map <Execution, Int>. Int is offset.
    // Also getTestBatches should receive the execution.
    private var offset = mutableMapOf<Execution, AtomicInteger>()

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
    fun getTestBatches(agentId: String): Mono<List<TestDto>> {
        val agent = agentRepository.findByAgentId(agentId) ?: error("The specified agent does not exist")
        val execution = agent.execution
        if (!offset.contains(execution)) {
            offset[execution] = AtomicInteger(0)
        }
        val tests = testRepository.retrieveBatches(configProperties.limit, offset[execution]!!.get()).map {
            TestDto(it.filePath, it.testSuite.id!!, it.id!!)
        }
        offset[execution]!!.addAndGet(configProperties.limit)
        return Mono.just(tests)
    }
}
