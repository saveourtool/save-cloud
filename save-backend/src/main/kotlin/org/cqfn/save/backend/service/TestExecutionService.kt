package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.TestExecutionRepository
import org.cqfn.save.backend.utils.toLocalDateTime

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val testResultRepository: TestExecutionRepository) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)

    /**
     * @param testExecutionsDto
     * @return list of lost tests
     */
    fun saveTestResult(testExecutionsDto: List<TestExecutionDto>): List<TestExecutionDto> {
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        testExecutionsDto.forEach { testExecDto ->
            val foundTestExec = testResultRepository.findById(testExecDto.id)
            foundTestExec.ifPresentOrElse({
                it.run {
                    this.startTime = testExecDto.startTime.toLocalDateTime()
                    this.endTime = testExecDto.endTime.toLocalDateTime()
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
