package org.cqfn.save.backend.service

import org.cqfn.save.backend.repository.ExecutionRepository
import org.cqfn.save.backend.repository.UserRepository
import org.cqfn.save.entities.Execution
import org.cqfn.save.entities.Organization
import org.cqfn.save.execution.ExecutionInitializationDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionUpdateDto

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

import java.time.LocalDateTime
import java.util.Optional

/**
 * Service that is used to manipulate executions
 */
@Service
class ExecutionService(private val executionRepository: ExecutionRepository,
                       private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(ExecutionService::class.java)

    /**
     * Find execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    fun findExecution(id: Long): Optional<Execution> = executionRepository.findById(id)

    /**
     * @param execution
     * @param username username of the user that has started the execution
     * @return id of the created [Execution]
     */
    @Suppress("UnsafeCallOnNullableType")  // hibernate should always assign ids
    fun saveExecution(execution: Execution, username: String): Long = executionRepository.save(execution.apply {
        this.user = userRepository.findByName(username).orElseThrow()
    }).id!!

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
     * @param name name of project
     * @param organization organization of project
     * @return list of execution dtos
     */
    fun getExecutionDtoByNameAndOrganization(name: String, organization: Organization) =
            executionRepository.getAllByProjectNameAndProjectOrganization(name, organization).map { it.toDto() }

    /**
     * Get latest (by start time an) execution by project name and organization
     *
     * @param name name of project
     * @param organizationId id of organization of project
     * @return execution or null if it was not found
     */
    fun getLatestExecutionByProjectNameAndProjectOrganizationId(name: String, organizationName: String): Optional<Execution> =
            executionRepository.findTopByProjectNameAndProjectOrganizationNameOrderByStartTimeDesc(name, organizationName)

    /**
     * @param executionInitializationDto execution dto to update
     * @return execution
     */
    fun updateNewExecution(executionInitializationDto: ExecutionInitializationDto) =
            executionRepository.findTopByProjectOrderByStartTimeDesc(
                executionInitializationDto.project
            )?.let { execution ->
                require(execution.version == null) { "Execution was already updated" }
                execution.version = executionInitializationDto.version
                execution.testSuiteIds = executionInitializationDto.testSuiteIds
                execution.resourcesRootPath = executionInitializationDto.resourcesRootPath
                execution.execCmd = executionInitializationDto.execCmd
                execution.batchSizeForAnalyzer = executionInitializationDto.batchSizeForAnalyzer
                executionRepository.save(execution)
            }

    /**
     * Delete all executions by project name and organization
     *
     * @param name name of project
     * @param organization organization of project
     * @return Unit
     */
    fun deleteExecutionByProjectNameAndProjectOrganization(name: String, organization: Organization) =
            executionRepository.getAllByProjectNameAndProjectOrganization(name, organization).forEach {
                executionRepository.delete(it)
            }

    /**
     * Delete all executions by project name and organization
     *
     * @param executionIds list of ids
     * @return Unit
     */
    fun deleteExecutionByIds(executionIds: List<Long>) =
            executionIds.forEach {
                executionRepository.deleteById(it)
            }

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

    /**
     * Map [execution] to a user by their name [username]
     *
     * @param execution
     * @param username
     */
    @Transactional
    fun updateExecutionWithUser(execution: Execution, username: String) {
        val user = userRepository.findByName(username).orElseThrow()
        executionRepository.save(execution.apply {
            this.user = user
        })
    }
}
