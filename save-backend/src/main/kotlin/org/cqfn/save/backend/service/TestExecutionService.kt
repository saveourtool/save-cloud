package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.toLocalTimeDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testResultRepository: TestExecutionRepository) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)

    /**
     * @param testExecutions list of test executions
     * @return list of lost tests
     */
    fun saveTestResult(testExecutionsDto: List<TestExecutionDto>): List<TestExecutionDto> {
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        testExecutionsDto.forEach { testExecDto ->
            val foundTestExec = testResultRepository.findById(testExecDto.id)
            foundTestExec.ifPresentOrElse({
                it.run {
                    this.startTime = testExecDto.startTime.toLocalTimeDate()
                    this.endTime = testExecDto.endTime.toLocalTimeDate()
                    this.status = testExecDto.status
                    testResultRepository.save(this)
                }
            },
                {
                    lostTests.add(testExecDto)
                    log.error("Test execution with ${testExecDto.id} id was not found")
                })
        }
        return lostTests
    }
}
