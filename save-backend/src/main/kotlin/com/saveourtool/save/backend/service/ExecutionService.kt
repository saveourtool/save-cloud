package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.Execution
import com.saveourtool.save.entities.Organization
import com.saveourtool.save.entities.Project
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.utils.blockingToMono
import com.saveourtool.save.utils.debug
import com.saveourtool.save.utils.asyncEffectIf
import com.saveourtool.save.utils.orNotFound

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

import java.time.LocalDateTime
import java.util.Optional

/**
 * Service that is used to manipulate executions
 */
@Suppress("LongParameterList")
@Service
class ExecutionService(
    private val executionRepository: ExecutionRepository,
    private val projectService: ProjectService,
    private val userRepository: UserRepository,
    private val testRepository: TestRepository,
    private val testExecutionRepository: TestExecutionRepository,
    @Lazy private val testSuitesService: TestSuitesService,
    private val configProperties: ConfigProperties,
    private val lnkContestProjectService: LnkContestProjectService,
    private val lnkContestExecutionService: LnkContestExecutionService,
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
     * @return created/updated [Execution]
     */
    fun saveExecution(execution: Execution): Execution = executionRepository.save(execution)

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

            if (execution.type == TestingType.CONTEST_MODE) {
                // maybe this execution is the new best execution under a certain contest
                lnkContestProjectService.updateBestExecution(execution)
            }

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
     * @param projectCoordinates
     * @param testSuiteIds
     * @param files
     * @param username
     * @param sdk
     * @param execCmd
     * @param batchSizeForAnalyzer
     * @param testingType
     * @return new [Execution] with provided values
     */
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    @Transactional
    fun createNew(
        projectCoordinates: ProjectCoordinates,
        testSuiteIds: List<Long>,
        files: List<FileKey>,
        username: String,
        sdk: Sdk,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Mono<Execution> {
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
            testingType = testingType,
            contestName,
        )
    }

    /**
     * @param execution
     * @param username
     * @return new [Execution] with values taken from [execution]
     */
    @Transactional
    fun createNewCopy(
        execution: Execution,
        username: String,
    ): Mono<Execution> = doCreateNew(
        project = execution.project,
        formattedTestSuiteIds = execution.testSuiteIds,
        version = execution.version,
        allTests = execution.allTests,
        additionalFiles = execution.additionalFiles,
        username = username,
        sdk = execution.sdk,
        execCmd = execution.execCmd,
        batchSizeForAnalyzer = execution.batchSizeForAnalyzer,
        testingType = execution.type,
        contestName = lnkContestExecutionService.takeIf { execution.type == TestingType.CONTEST_MODE }
            ?.findContestByExecution(execution)?.name,
    )

    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "UnsafeCallOnNullableType")
    private fun doCreateNew(
        project: Project,
        formattedTestSuiteIds: String?,
        version: String?,
        allTests: Long,
        additionalFiles: String,
        username: String,
        sdk: String,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Mono<Execution> {
        val user = userRepository.findByName(username).orNotFound {
            "Not found user $username"
        }
        val testSuiteSourceName = testSuitesService.getById(
            Execution.parseAndGetTestSuiteIds(formattedTestSuiteIds)!!.first()
        ).source.name
        val execution = Execution(
            project = project,
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.PENDING,
            testSuiteIds = formattedTestSuiteIds,
            batchSize = configProperties.initialBatchSize,
            type = testingType,
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
            testSuiteSourceName = testSuiteSourceName,
            score = null,
        )
        return blockingToMono {
            saveExecution(execution)
        }
            .asyncEffectIf({ testingType == TestingType.CONTEST_MODE }) { savedExecution ->
                lnkContestExecutionService.createLink(
                    savedExecution, requireNotNull(contestName) {
                        "Requested execution type is ${TestingType.CONTEST_MODE} but no contest name has been specified"
                    }
                )
            }
            .doOnSuccess { savedExecution ->
                log.info("Created a new execution id=${savedExecution.id} for project id=${project.id}")
            }
    }
}
