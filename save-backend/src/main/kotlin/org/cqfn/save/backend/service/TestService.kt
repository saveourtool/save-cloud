package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.entities.Test
import org.cqfn.save.test.TestDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService(private val configProperties: ConfigProperties) {
    private val log = LoggerFactory.getLogger(TestService::class.java)

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var executionRepository: ExecutionRepository

    /**
     * @param tests
     */
    fun saveTests(tests: List<Test>) {
        testRepository.saveAll(tests)
    }

    /**
     * @param agentId
     * @return Test batches
     */
    @Transactional
    fun getTestBatches(agentId: String): Mono<List<TestDto>> {
        val agent = agentRepository.findByContainerId(agentId) ?: error("The specified agent does not exist")
        log.debug("Agent found: $agent")
        val execution = agent.execution
        log.debug("Retrieving tests")
        val tests = testRepository.retrieveBatches(execution.executionLimit, execution.offset).map {
            TestDto(it.filePath, it.testSuite.id!!, it.id!!)
        }
        log.debug("Increasing offset of execution - ${agent.execution}")
        executionRepository.setNewOffset(execution.offset + execution.executionLimit, execution.id!!)
        return Mono.just(tests)
    }
}
