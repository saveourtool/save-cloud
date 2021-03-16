package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.entities.TestExecution
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testResultRepository: TestExecutionRepository) {
    /**
     * @param testExecutions list of test executions
     */
    fun saveTestResult(testExecutions: List<TestExecution>) {
        testExecutions.forEach { testExec ->
            testResultRepository.run {
                val foundTestExec = this.findById(testExec.id)
                 if (!foundTestExec.isPresent) {
                     println("error")
                 } else {
                     foundTestExec.map {
                         it.startTime = testExec.startTime
                         it.endTime = testExec.endTime
                         it.status = testExec.status
                         this.save(it)
                     }
                }
            }
        }
    }
}
