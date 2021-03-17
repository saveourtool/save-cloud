package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.entities.TestExecution
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testResultRepository: TestExecutionRepository) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)

    /**
     * @param testExecutions list of test executions
     */
    fun saveTestResult(testExecutions: List<TestExecution>): List<TestExecution> {
        val lostTests = mutableListOf<TestExecution>()
        testExecutions.forEach { testExec ->
            val foundTestExec = testResultRepository.findById(testExec.id)
            foundTestExec.ifPresentOrElse(
                {
                    it.run {
                        this.startTime = testExec.startTime
                        this.endTime = testExec.endTime
                        this.status = testExec.status
                        testResultRepository.save(this)
                    }
                },
                {
                    lostTests.add(testExec)
                    log.error("Test execution with ${testExec.id} id was not found")
                })
        }
        return lostTests
    }
}
