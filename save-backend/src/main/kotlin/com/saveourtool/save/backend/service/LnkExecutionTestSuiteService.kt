package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.repository.LnkExecutionTestSuiteRepository
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.LnkExecutionTestSuite
import com.saveourtool.save.entities.TestSuite
import org.springframework.stereotype.Service

/**
 * Service of [LnkExecutionTestSuite]
 */
@Service
class LnkExecutionTestSuiteService(
    private val lnkExecutionTestSuiteRepository: LnkExecutionTestSuiteRepository,
) {
    /**
     * @param execution
     * @return all [TestSuite]s with rights for [execution]
     */
    fun getAllTestSuitesByExecution(execution: Execution) =
            lnkExecutionTestSuiteRepository.findByExecution(execution)
                .map {
                    it.testSuite
                }

    /**
     * @param executionId
     * @return all [TestSuite]s with rights for [executionId]
     */
    fun getAllTestSuiteIdsByExecutionId(executionId: Long) =
            lnkExecutionTestSuiteRepository.findByExecutionId(executionId)
                .map {
                    it.testSuite.requiredId()
                }

    /**
     * @param testSuiteId
     * @return all [Execution]s with rights for [testSuiteId]
     */
    fun getAllExecutionsByTestSuiteId(testSuiteId: Long) = lnkExecutionTestSuiteRepository.findByTestSuiteId(testSuiteId)
        .map {
            it.execution
        }

    /**
     * @param lnkExecutionTestSuite
     */
    fun save(lnkExecutionTestSuite: LnkExecutionTestSuite): LnkExecutionTestSuite = lnkExecutionTestSuiteRepository.save(lnkExecutionTestSuite)

    /**
     * @param lnkExecutionTestSuites
     */
    fun saveAll(lnkExecutionTestSuites: List<LnkExecutionTestSuite>): List<LnkExecutionTestSuite> = lnkExecutionTestSuiteRepository.saveAll(lnkExecutionTestSuites)
}
