package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionInitializationDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.ExecutionType
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.orNotFound

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
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
class ExecutionService(
    private val executionRepository: ExecutionRepository,
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    private val testSuitesService: TestSuitesService,
    private val configProperties: ConfigProperties,
) {
    private val log = LoggerFactory.getLogger(ExecutionService::class.java)

    /**
     * Find execution by id
     *
     * @param id id of execution
     * @return execution if it has been found
     */
    fun findExecution(id: Long): Execution? = executionRepository.findByIdOrNull(id)

    /**
     * @param execution
     * @param username username of the user that has started the execution
     * @return id of the created [Execution]
     */
    fun saveExecutionAndReturnId(execution: Execution, username: String): Long = executionRepository.save(execution.apply {
        this.user = userRepository.findByName(username).orNotFound()
    }).requiredId()

    /**
     * @param execution
     * @return id of the created [Execution]
     */
    fun saveExecutionAndReturnId(execution: Execution): Long = saveExecution(execution).requiredId()

    /**
     * @param execution
     * @return created/updated [Execution]
     */
    fun saveExecution(execution: Execution): Execution = executionRepository.save(execution)

    /**
     * @param id [Execution.id]
     * @param newStatus [Execution.status]
     * @throws ResponseStatusException
     */
    @Transactional
    fun updateExecutionStatusById(id: Long, newStatus: ExecutionStatus) {
        log.debug("Updating status to $newStatus on execution id = $id")
        val execution = executionRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }.apply {
            status = newStatus
        }
        if (execution.status == ExecutionStatus.FINISHED || execution.status == ExecutionStatus.ERROR) {
            // execution is completed, we can update end time
            execution.endTime = LocalDateTime.now()
            // if the tests are stuck in the READY_FOR_TESTING or RUNNING status
            testExecutionRepository.findByStatusListAndExecutionId(listOf(TestResultStatus.READY_FOR_TESTING, TestResultStatus.RUNNING), id).map { testExec ->
                log.debug {
                    "Test execution id=${testExec.id} has status ${testExec.status} while execution id=${execution.id} has status ${execution.status}. " +
                            "Will mark it ${TestResultStatus.INTERNAL_ERROR}"
                }
                testExec.status = TestResultStatus.INTERNAL_ERROR
                testExecutionRepository.save(testExec)
            }
        }
        executionRepository.save(execution)
    }

    /**
     * @param execution [Execution]
     * @param newStatus [Execution.status]
     * @throws ResponseStatusException
     */
    @Transactional
    fun updateExecutionStatus(execution: Execution, newStatus: ExecutionStatus) {
        log.debug("Updating status to $newStatus on execution id = ${execution.requiredId()}")
        val updatedExecution = execution.apply {
            status = newStatus
        }
        if (updatedExecution.status == ExecutionStatus.FINISHED || updatedExecution.status == ExecutionStatus.ERROR) {
            // execution is completed, we can update end time
            updatedExecution.endTime = LocalDateTime.now()
            // if the tests are stuck in the READY_FOR_TESTING or RUNNING status
            testExecutionRepository.findByStatusListAndExecutionId(listOf(TestResultStatus.READY_FOR_TESTING, TestResultStatus.RUNNING), execution.requiredId()).map { testExec ->
                log.debug {
                    "Test execution id=${testExec.id} has status ${testExec.status} while execution id=${updatedExecution.id} has status ${updatedExecution.status}. " +
                            "Will mark it ${TestResultStatus.INTERNAL_ERROR}"
                }
                testExec.status = TestResultStatus.INTERNAL_ERROR
                testExecutionRepository.save(testExec)
            }
        }
        executionRepository.save(updatedExecution)
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
     * @param organizationName name of organization of project
     * @return execution or null if it was not found
     */
    fun getLatestExecutionByProjectNameAndProjectOrganizationName(name: String, organizationName: String): Optional<Execution> =
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
                execution.formatAndSetTestSuiteIds(executionInitializationDto.testSuiteIds)
                execution.allTests = executionInitializationDto.testSuiteIds
                    .flatMap { testRepository.findAllByTestSuiteId(it) }
                    .count()
                    .toLong()
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
     * Get all executions, which contains provided test suite id
     *
     * @param testSuiteId
     * @return list of [Execution]'s
     */
    fun getExecutionsByTestSuiteId(testSuiteId: Long): List<Execution> = executionRepository.findAllByTestSuiteIdsContaining(testSuiteId.toString())

    /**
     * @param execution execution, tests metrics of which should be reset
     */
    fun resetMetrics(execution: Execution) {
        execution.apply {
            runningTests = 0
            passedTests = 0
            failedTests = 0
            skippedTests = 0
            unmatchedChecks = 0
            matchedChecks = 0
            expectedChecks = 0
            unexpectedChecks = 0
        }
        saveExecutionAndReturnId(execution)
    }

    /**
     * Map [execution] to a user by their name [username]
     *
     * @param execution
     * @param username
     */
    @Transactional
    fun updateExecutionWithUser(execution: Execution, username: String) {
        val user = userRepository.findByName(username).orNotFound()
        executionRepository.save(execution.apply {
            this.user = user
        })
    }

    @Suppress("LongParameterList")
    @Transactional
    fun createNew(
        projectCoordinates: ProjectCoordinates,
        testSuiteIds: List<Long>,
        files: List<FileKey>,
        username: String,
        sdk: Sdk,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
    ): Execution {
        val project = with(projectCoordinates) {
            projectService.findByNameAndOrganizationName(projectName, organizationName).orNotFound {
                "Not found project $projectName in $organizationName"
            }
        }
        return doCreateNew(
            project = project,
            formattedTestSuiteIds = Execution.formatTestSuiteIds(testSuiteIds),
            version = testSuitesService.getSingleVersionByIds(testSuiteIds),
            allTests = testSuiteIds.flatMap { testRepository.findAllByTestSuiteId(it) }
                .count()
                .toLong(),
            additionalFiles = files.format(),
            username = username,
            sdk = sdk.toString(),
            execCmd = execCmd,
            batchSizeForAnalyzer = batchSizeForAnalyzer,
        )
    }

    @Transactional
    fun createNewCopy(
        execution: Execution,
        username: String,
    ): Execution {
        return doCreateNew(
            project = execution.project,
            formattedTestSuiteIds = execution.testSuiteIds,
            version = execution.version,
            allTests = execution.allTests,
            additionalFiles = execution.additionalFiles,
            username = username,
            sdk = execution.sdk,
            execCmd = execution.execCmd,
            batchSizeForAnalyzer = execution.batchSizeForAnalyzer,
        )
    }

    @Suppress("LongParameterList")
    fun doCreateNew(
        project: Project,
        formattedTestSuiteIds: String?,
        version: String?,
        allTests: Long,
        additionalFiles: String,
        username: String,
        sdk: String,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
    ): Execution {
        val user = userRepository.findByName(username).orNotFound {
            "Not found user $username"
        }
        val execution = Execution(
            project = project,
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.PENDING,
            testSuiteIds = formattedTestSuiteIds,
            batchSize = configProperties.initialBatchSize,
            // FIXME: remove this type
            type = ExecutionType.GIT,
            version = version,
            allTests = allTests,
            runningTests = 0,
            passedTests = 0,
            failedTests = 0,
            skippedTests = 0,
            unmatchedChecks = 0,
            matchedChecks = 0,
            expectedChecks = 0,
            unexpectedChecks = 0,
            sdk = sdk,
            additionalFiles = additionalFiles,
            user = user,
            execCmd = execCmd,
            batchSizeForAnalyzer = batchSizeForAnalyzer,
        )
        val savedExecution = executionRepository.save(execution)
        log.info("Created a new execution id=${savedExecution.id} for project id=${project.id}")
        return savedExecution
    }
}
