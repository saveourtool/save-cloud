package org.cqfn.save.backend.service

import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.backend.repository.BaseEntityRepository
import org.cqfn.save.backend.utils.toLocalDateTime
import org.cqfn.save.entities.TestExecution

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for test result
 */
@Service
class TestExecutionService(private val baseEntityRepository: BaseEntityRepository<TestExecution>) {
    private val log = LoggerFactory.getLogger(TestExecutionService::class.java)

    /**
     * @param testExecutionsDtos
     * @return list of lost tests
     */
    fun saveTestResult(testExecutionsDtos: List<TestExecutionDto>): List<TestExecutionDto> {
        val lostTests: MutableList<TestExecutionDto> = mutableListOf()
        testExecutionsDtos.forEach { testExecDto ->
            val foundTestExec = baseEntityRepository.findById(testExecDto.id)
            foundTestExec.ifPresentOrElse({
                it.run {
                    this.startTime = testExecDto.startTime.toLocalDateTime()
                    this.endTime = testExecDto.endTime.toLocalDateTime()
                    this.status = testExecDto.status
                    baseEntityRepository.save(this)
                }
            },
                {
                    lostTests.add(testExecDto)
                    log.error("Test execution with id=[${testExecDto.id}] was not found in the DB")
                })
        }
        return lostTests
    }
}
