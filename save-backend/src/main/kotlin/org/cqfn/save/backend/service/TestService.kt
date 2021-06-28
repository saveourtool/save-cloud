package org.cqfn.save.backend.service

import org.cqfn.save.backend.configs.ConfigProperties
import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.backend.repository.TestSuiteRepository
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Test
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

import java.time.LocalDateTime

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

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    @Autowired
    private lateinit var testSuiteRepository: TestSuiteRepository

    /**
     * @param tests
     * @return list tests id's
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTests(tests: List<TestDto>): List<Long> {
        val testsId: MutableCollection<Long> = mutableListOf()
        tests.forEach { testDto ->
            testRepository.findByHash(testDto.hash!!)?.let {
                log.debug("Test $testDto is already present with id=${it.id}")
                testsId.add(it.id!!)
            } ?: run {
                log.debug("Test $testDto is not found in the DB, will save it")
                val testSuite = TestSuite(propertiesRelativePath = "FB").apply {
                    id = testDto.testSuiteId
                }
                Test(testDto.hash!!, testDto.filePath, LocalDateTime.now(), testSuite).run {
                    testRepository.save(this)
                    testsId.add(this.id!!)
                }
            }
        }
        return testsId.toList()
    }

    /**
     * @param agentId
     * @return Test batches
     */
    @Transactional
    @Suppress("UnsafeCallOnNullableType")
    fun getTestBatches(agentId: String): Mono<TestBatch> {
        val agent = agentRepository.findByContainerId(agentId) ?: error("The specified agent does not exist")
        log.debug("Agent found: $agent")
        val execution = agent.execution
        log.debug("Retrieving tests")
        val tests = testExecutionRepository.findByStatusAndExecutionId(
            TestResultStatus.READY,
            execution.id!!,
            PageRequest.of(execution.page, execution.batchSize)
        )
        val testDtos = tests.map {
            TestDto(it.test.filePath, it.test.testSuite.id!!, null)
        }
        log.debug("Increasing offset of the execution - ${agent.execution}")
        ++execution.page
        executionRepository.save(execution)
        return Mono.just(TestBatch(testDtos, tests.map { it.test.testSuite }.associate {
            it.id!! to "--properties-file ${it.propertiesRelativePath}"
        }))
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestService::class.java)
    }
}
