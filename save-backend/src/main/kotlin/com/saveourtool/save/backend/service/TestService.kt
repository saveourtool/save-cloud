package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.AgentRepository
import com.saveourtool.save.backend.repository.ExecutionRepository
import com.saveourtool.save.backend.repository.TestExecutionRepository
import com.saveourtool.save.backend.repository.TestRepository
import com.saveourtool.save.domain.TestResultStatus
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Test
import com.saveourtool.save.entities.TestExecution
import com.saveourtool.save.entities.TestSuite
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.test.TestBatch
import com.saveourtool.save.test.TestDto

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
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
     * @return list tests
     */
    @Suppress("UnsafeCallOnNullableType")
    fun saveTests(tests: List<TestDto>): List<Test> {
        val (existingTests, nonExistentTests) = tests.map { testDto ->
            // only match fields that are present in DTO
            testRepository.findByHashAndFilePathAndTestSuiteIdAndPluginName(testDto.hash, testDto.filePath, testDto.testSuiteId, testDto.pluginName).map {
                log.debug("Test $testDto is already present with id=${it.id} and testSuiteId=${testDto.testSuiteId}")
                it
            }
                .orElseGet {
                    log.trace("Test $testDto is not found in the DB, will save it")
                    val testSuiteStub = TestSuite(testRootPath = "N/A").apply {
                        id = testDto.testSuiteId
                    }
                    Test(testDto.hash, testDto.filePath, testDto.pluginName, LocalDateTime.now(), testSuiteStub, testDto.tags.joinToString(";"))
                }
        }
            .partition { it.id != null }
        testRepository.saveAll(nonExistentTests)
        return existingTests + nonExistentTests
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
        val executionId = agent.execution.id!!
        val lock = locks.computeIfAbsent(executionId) { Any() }
        return synchronized(lock) {
            log.debug("Acquired lock for executionId=$executionId")
            val testExecutions = transactionTemplate.execute {
                val execution = executionRepository.getById(executionId)
                getTestExecutionsBatchByExecutionIdAndUpdateStatus(execution)
            }!!
            val testDtos = testExecutions.map { it.test.toDto() }
            Mono.fromCallable {
                val testBatch = TestBatch(testDtos, testExecutions.map { it.test.testSuite }.associate {
                    it.id!! to it.testRootPath
                })
                log.debug("Releasing lock for executionId=$executionId")
                testBatch
            }
        }
    }

    /**
     * @param testSuiteId
     * @return tests with provided [testSuiteId]
     */
    fun findTestsByTestSuiteId(testSuiteId: Long) =
            testRepository.findAllByTestSuiteId(testSuiteId)

    /**
     * @param executionId
     * @return all tests which has testSuiteId from [execution][com.saveourtool.save.entities.Execution] found by provided [executionId]
     */
    fun findTestsByExecutionId(executionId: Long): List<Test> {
        val execution = executionRepository.findById(executionId).orElseGet {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Execution (id=$executionId) not found")
        }
        return execution.parseAndGetTestSuiteIds()?.flatMap { findTestsByTestSuiteId(it) }
            ?: throw ResponseStatusException(HttpStatus.CONFLICT, "Execution (id=${executionId}) doesn't contain testSuiteIds")
    }

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
        val newRunningTestExecutions = testExecutions.onEach { testExecution ->
            testExecutionRepository.save(testExecution.apply {
                status = TestResultStatus.RUNNING
            })
        }.count()
        executionRepository.saveAndFlush(execution.apply {
            log.debug("Updating counter for running tests: $runningTests -> ${runningTests + newRunningTestExecutions}")
            runningTests += newRunningTestExecutions
        })
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
