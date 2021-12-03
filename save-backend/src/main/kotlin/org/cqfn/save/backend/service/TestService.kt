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
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import reactor.core.publisher.Mono

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service that is used for manipulating data with tests
 */
@Service
class TestService(
    private val testRepository: TestRepository,
    private val agentRepository: AgentRepository,
    private val executionRepository: ExecutionRepository,
    private val testExecutionRepository: TestExecutionRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val locks: ConcurrentHashMap<Long, Any> = ConcurrentHashMap()
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = PROPAGATION_REQUIRES_NEW
    }

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
                val testSuiteStub = TestSuite(testRootPath = "N/A").apply {
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
            val testExecutions = transactionTemplate.execute {
                getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution)
            }!!
            val testDtos = testExecutions.map { it.test.toDto() }
            Mono.just(TestBatch(testDtos, testExecutions.map { it.test.testSuite }.associate {
                it.id!! to it.testRootPath
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
     * Retrieves a batch of test executions with status `READY_FOR_TESTING` from the datasource and sets their statuses to `RUNNING`
     *
     * @param execution execution for which a batch is requested
     * @return a batch of [batchSize] tests with status `READY_FOR_TESTING`
     */
    @Suppress("UnsafeCallOnNullableType")
    internal fun getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution: Execution): List<TestExecution> {
        val executionId = execution.id!!
        val batchSize = execution.batchSize!!
        val pageRequest = PageRequest.of(0, batchSize)
        val testExecutions = testExecutionRepository.findByStatusAndExecutionId(
            TestResultStatus.READY_FOR_TESTING,
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
