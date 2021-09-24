package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
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

import java.io.File
import java.time.LocalDateTime

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService {
    private val log = LoggerFactory.getLogger(TestService::class.java)

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var agentRepository: AgentRepository

    @Autowired
    private lateinit var executionRepository: ExecutionRepository

    @Autowired
    private lateinit var testExecutionRepository: TestExecutionRepository

    /**
     * @param tests
     * @return list tests id's
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTests(tests: List<TestDto>): List<Long> = tests.map { testDto ->
        // only match fields that are present in DTO
        testRepository.findByHashAndFilePathAndTestSuiteId(testDto.hash, testDto.filePath, testDto.testSuiteId).map {
            log.debug("Test $testDto is already present with id=${it.id} and testSuiteId=${it.testSuite.id}")
            it
        }
            .orElseGet {
                log.debug("Test $testDto is not found in the DB, will save it")
                val testSuiteStub = TestSuite(propertiesRelativePath = "FB").apply {
                    id = testDto.testSuiteId
                }
                testRepository.save(
                    Test(testDto.hash, testDto.filePath, testDto.pluginName, LocalDateTime.now(), testSuiteStub)
                )
            }
            .id!!
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
            PageRequest.of(execution.page, execution.batchSize!!)
        )
        val testDtos = tests.map {
            TestDto(it.test.filePath, it.test.pluginName, it.test.testSuite.id!!, it.test.hash)
        }
        log.debug("Increasing offset of the execution - ${agent.execution}")
        ++execution.page
        executionRepository.save(execution)
        return Mono.just(TestBatch(testDtos, tests.map { it.test.testSuite }.associate {
            it.id!! to "${File(it.propertiesRelativePath).parent}"
        }))
    }

    /**
     * @param testSuiteId
     * @return tests with provided [testSuiteId]
     */
    fun findTestsByTestSuiteId(testSuiteId: Long) =
            testRepository.findAllByTestSuiteId(testSuiteId)

    companion object {
        private val log = LoggerFactory.getLogger(TestService::class.java)
    }
}
