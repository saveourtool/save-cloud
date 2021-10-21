package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.AgentRepository
import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.repository.TestRepository
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Test
import org.cqfn.save.entities.TestExecution
import org.cqfn.save.entities.TestSuite
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.test.TestBatch
import org.cqfn.save.test.TestDto

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService {
    private val locks: ConcurrentHashMap<Long, Any> = ConcurrentHashMap()

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
                val testSuiteStub = TestSuite(testRootPath = "Undefined").apply {
                    id = testDto.testSuiteId
                }
                testRepository.save(
                    Test(testDto.hash, testDto.filePath, testDto.pluginName, LocalDateTime.now(), testSuiteStub, testDto.tags.joinToString(";"))
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
        val lock = locks.computeIfAbsent(execution.id!!) { Any() }
        return synchronized(lock) {
            val testExecutions = getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution)
            val testDtos = testExecutions.map { it.test.toDto() }
            Mono.just(TestBatch(testDtos, testExecutions.map { it.test.testSuite }.associate {
                it.id!! to File(it.propertiesRelativePath).parent
            }))
        }
    }

    /**
     * @param testSuiteId
     * @return tests with provided [testSuiteId]
     */
    fun findTestsByTestSuiteId(testSuiteId: Long) =
            testRepository.findAllByTestSuiteId(testSuiteId)

    /**
     * Retrieves a batch of test executions with status `READY` from the datasource and sets their statuses to `RUNNING`
     *
     * @param execution execution for which a batch is requested
     * @return a batch of [batchSize] tests with status `READY`
     */
    @Suppress("UnsafeCallOnNullableType")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    internal fun getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution: Execution): List<TestExecution> {
        val executionId = execution.id!!
        val batchSize = execution.batchSize!!
        val pageRequest = PageRequest.of(0, batchSize)
        val testExecutions = testExecutionRepository.findByStatusAndExecutionId(
            TestResultStatus.READY,
            executionId,
            pageRequest
        )
        log.debug("Retrieved ${testExecutions.size} tests for page request $pageRequest, test IDs: ${testExecutions.map { it.id!! }}")
        testExecutions.forEach {
            testExecutionRepository.save(it.apply {
                status = TestResultStatus.RUNNING
            })
            executionRepository.save(execution.apply {
                runningTests++
            })
        }
        return testExecutions
    }

    /**
     * Remove execution ids from [locks] for executions that are no more running
     */
    @Scheduled(cron = "0 0/50 * * * ?")
    @Suppress("UnsafeCallOnNullableType")
    fun cleanupLocks() {
        log.debug("Starting scheduled task of `locks` map cleanup")
        executionRepository.findAllById(locks.keys).forEach {
            if (it.status != ExecutionStatus.RUNNING) {
                log.debug("Will remove key=[${it.id!!}] from the map, because execution state is ${it.status}")
                locks.remove(it.id!!)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestService::class.java)
    }
}
