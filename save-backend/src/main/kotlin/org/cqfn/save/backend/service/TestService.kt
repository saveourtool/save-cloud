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
                    Test(testDto.hash, testDto.filePath, testDto.pluginName, LocalDateTime.now(), testSuiteStub, testDto.tags!!.joinToString(";"))
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
        log.debug("Agent found, id=${agent.id}")
        val execution = agent.execution
        val pageRequest = PageRequest.of(execution.page, execution.batchSize!!)
        log.debug("Retrieving tests for page request $pageRequest")
        val testExecutions = testExecutionRepository.findByStatusAndExecutionId(
            TestResultStatus.READY,
            execution.id!!,
            pageRequest
        )
        val testDtos = testExecutions.map {
            val tagsList = it.test.tags?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
            TestDto(it.test.filePath, it.test.pluginName, it.test.testSuite.id!!, it.test.hash, tagsList)
        }
        log.debug("Increasing offset of the execution - from ${execution.page} by ${execution.batchSize}")
        ++execution.page
        executionRepository.save(execution)
        return Mono.just(TestBatch(testDtos, testExecutions.map { it.test.testSuite }.associate {
            it.id!! to File(it.propertiesRelativePath).parent
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
