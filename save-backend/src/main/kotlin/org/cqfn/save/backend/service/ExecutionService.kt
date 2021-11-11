package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.ProjectRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

import java.time.LocalDateTime
import java.util.Optional

/**
 * Service that is used to manipulate executions
 */
@Service
class ExecutionService(private val executionRepository: ExecutionRepository) {
    private val log = LoggerFactory.getLogger(ExecutionService::class.java)

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    /**
     * Find execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    fun findExecution(id: Long) = executionRepository.findById(id)

    /**
     * @param execution
     * @return id of the created [Execution]
     */
    @Suppress("UnsafeCallOnNullableType")  // hibernate should always assign ids
    fun saveExecution(execution: Execution): Long = executionRepository.save(execution).id!!

    /**
     * @param execution
     * @throws ResponseStatusException
     */
    fun updateExecution(execution: ExecutionUpdateDto) {
        executionRepository.findById(execution.id).ifPresentOrElse({
            it.status = execution.status
            if (it.status == ExecutionStatus.FINISHED || it.status == ExecutionStatus.ERROR) {
                // execution is completed, we can update end time
                it.endTime = LocalDateTime.now()
            }
            executionRepository.save(it)
        }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * @param execution
     * @throws ResponseStatusException
     */
    @Suppress("UnsafeCallOnNullableType")
    fun updateExecution(execution: Execution) {
        executionRepository.findById(execution.id!!).ifPresentOrElse({
            executionRepository.save(execution)
        }) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }

    /**
     * @param executionId id of execution
     * @return execution dto based on id
     */
    fun getExecutionDto(executionId: Long): ExecutionDto? {
        var executionDto: ExecutionDto? = null
        executionRepository.findById(executionId).ifPresentOrElse({
            executionDto = it.toDto()
        }) {
            log.error("Can't find execution by id = $executionId")
        }
        return executionDto
    }

    /**
     * @param name name of project
     * @param owner owner of project
     * @return list of execution dtos
     */
    fun getExecutionDtoByNameAndOwner(name: String, owner: String) =
            executionRepository.getAllByProjectNameAndProjectOwner(name, owner).map { it.toDto() }

    /**
     * Get latest (by start time an) execution by project name and project owner
     *
     * @param name name of project
     * @param owner owner of project
     * @return execution or null if it was not found
     */
    fun getLatestExecutionByProjectNameAndProjectOwner(name: String, owner: String): Optional<Execution> =
            executionRepository.findTopByProjectNameAndProjectOwnerOrderByStartTimeDesc(name, owner)

    /**
     * @param executionInitializationDto execution dto to update
     * @return execution
     */
    fun updateNewExecution(executionInitializationDto: ExecutionInitializationDto) =
            executionRepository.findTopByProjectOrderByStartTimeDesc(executionInitializationDto.project)?.let {
                require(it.version == null) { "Execution was already updated" }
                it.version = executionInitializationDto.version
                it.testSuiteIds = executionInitializationDto.testSuiteIds
                it.resourcesRootPath = executionInitializationDto.resourcesRootPath
                executionRepository.save(it)
            }

    /**
     * Delete all executions by project name and project owner
     *
     * @param name name of project
     * @param owner owner of project
     * @return Unite
     */
    fun deleteExecutionByProjectNameAndProjectOwner(name: String, owner: String) =
            executionRepository.getAllByProjectNameAndProjectOwner(name, owner).forEach {
                executionRepository.delete(it)
            }.let { executionRepository.flush() }

    /**
     * Delete all executions by project name and project owner
     *
     * @param executionIds list of ids
     * @return Unite
     */
    fun deleteExecutionByIds(executionIds: List<Long>) =
            executionIds.forEach {
                executionRepository.deleteById(it)
            }.let { executionRepository.flush() }

    /**
     * @param execution execution, tests metrics of which should be reset
     */
    fun resetMetrics(execution: Execution) {
        execution.apply {
            runningTests = 0
            passedTests = 0
            failedTests = 0
            skippedTests = 0
        }
        saveExecution(execution)
    }
}
