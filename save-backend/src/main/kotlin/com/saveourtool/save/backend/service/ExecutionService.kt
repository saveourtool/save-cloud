package com.saveourtool.save.backend.service

import com.saveourtool.save.backend.configs.ConfigProperties
import com.saveourtool.save.backend.repository.*
import com.saveourtool.save.domain.*
import com.saveourtool.save.entities.*
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.utils.*

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

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
    private val lnkExecutionTestSuiteService: LnkExecutionTestSuiteService,
    @Lazy private val fileService: FileService,
    private val lnkExecutionFileRepository: LnkExecutionFileRepository,
    private val agentService: AgentService,
    private val agentStatusService: AgentStatusService,
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
     * Get execution by id
     *
     * @param id id of [Execution]
     * @return [Execution] or exception
     */
    fun getExecution(id: Long): Execution = findExecution(id).orNotFound {
        "Not found execution with id $id"
    }

    /**
     * @param execution execution that is connected to testSuite
     * @param testSuites list of manageable [TestSuite]
     * @param files list of manageable [File]
     */
    @Transactional
    fun save(execution: Execution, testSuites: Collection<TestSuite>, files: Collection<File> = emptyList()): Execution {
        val newExecution = executionRepository.save(execution)
        testSuites.map { LnkExecutionTestSuite(newExecution, it) }.let { lnkExecutionTestSuiteService.saveAll(it) }
        files.map { LnkExecutionFile(newExecution, it) }.let { lnkExecutionFileRepository.saveAll(it) }
        return newExecution
    }

    /**
     * @param srcExecution [Execution]
     * @param newStatus [Execution.status]
     * @throws ResponseStatusException
     */
    @Transactional
    fun updateExecutionStatus(srcExecution: Execution, newStatus: ExecutionStatus) {
        log.debug("Updating status to $newStatus on execution id = ${srcExecution.requiredId()}")
        val execution = executionRepository.findWithLockingById(srcExecution.requiredId()).orNotFound()
        val updatedExecution = execution.apply {
            status = newStatus
        }
        if (updatedExecution.status == ExecutionStatus.FINISHED || updatedExecution.status == ExecutionStatus.ERROR) {
            // execution is completed, we can update end time
            updatedExecution.endTime = LocalDateTime.now()

            if (execution.type == TestingType.CONTEST_MODE && updatedExecution.status == ExecutionStatus.FINISHED) {
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
     * @return list of execution
     */
    fun getExecutionByNameAndOrganization(name: String, organization: Organization) =
            executionRepository.getAllByProjectNameAndProjectOrganization(name, organization)

    /**
     * @param name name of project
     * @param organization organization of project
     * @param start start date
     * @param end end date
     * @return list of executions
     */
    fun getExecutionByNameAndOrganizationAndStartTimeBetween(
        name: String,
        organization: Organization,
        start: LocalDateTime,
        end: LocalDateTime
    ) =
            executionRepository.findByProjectNameAndProjectOrganizationAndStartTimeBetween(name, organization, start, end)

    /**
     * @param name name of project
     * @param organization organization of project
     * @return list of execution dtos
     */
    fun getExecutionDtoByNameAndOrganization(name: String, organization: Organization) = getExecutionByNameAndOrganization(name, organization).map { it.toDto() }

    /**
     * @param name name of project
     * @param organization organization of project
     * @return list of execution
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun getExecutionNotParticipatingInContestByNameAndOrganization(name: String, organization: Organization) =
            executionRepository.getAllByProjectNameAndProjectOrganization(name, organization).filter {
                lnkContestExecutionService.findContestByExecution(it) == null
            }

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
     * Delete executions, except participating in contests, by project name and organization
     *
     * @param name name of project
     * @param organization organization of project
     * @return Unit
     */
    @Suppress("IDENTIFIER_LENGTH")
    fun deleteExecutionExceptParticipatingInContestsByProjectNameAndProjectOrganization(name: String, organization: Organization) {
        getExecutionNotParticipatingInContestByNameAndOrganization(name, organization).forEach {
            executionRepository.delete(it)
        }
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
    fun getExecutionsByTestSuiteId(testSuiteId: Long): List<Execution> =
            lnkExecutionTestSuiteService.getAllExecutionsByTestSuiteId(testSuiteId)

    /**
     * @param projectCoordinates
     * @param testSuiteIds
     * @param fileIds
     * @param username
     * @param sdk
     * @param execCmd
     * @param batchSizeForAnalyzer
     * @param testingType
     * @param contestName
     * @return new [Execution] with provided values
     */
    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS")
    @Transactional
    fun createNew(
        projectCoordinates: ProjectCoordinates,
        testSuiteIds: List<Long>,
        fileIds: List<Long>,
        username: String,
        sdk: Sdk,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Execution {
    ): Mono<Execution> = blockingToMono {
        val project = with(projectCoordinates) {
            projectService.findByNameAndOrganizationNameAndCreatedStatus(projectName, organizationName).orNotFound {
                "Not found project $projectName in $organizationName"
            }
        }
        doCreateNew(
            project = project,
            testSuites = testSuiteIds.map { testSuitesService.getById(it) },
            allTests = testSuiteIds.flatMap { testRepository.findAllByTestSuiteId(it) }
                .count()
                .toLong(),
            files = fileIds.map { fileService.get(it) },
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
    ): Mono<Execution> = blockingToMono {
        val testSuites = lnkExecutionTestSuiteService.getAllTestSuitesByExecution(execution)
        val files = lnkExecutionFileRepository.findAllByExecutionId(execution.requiredId()).map { it.file }
        doCreateNew(
            project = execution.project,
            testSuites = testSuites,
            allTests = execution.allTests,
            files = files,
            username = username,
            sdk = execution.sdk,
            execCmd = execution.execCmd,
            batchSizeForAnalyzer = execution.batchSizeForAnalyzer,
            testingType = execution.type,
            contestName = lnkContestExecutionService.takeIf { execution.type == TestingType.CONTEST_MODE }
                ?.findContestByExecution(execution)?.name,
        )
    }

    @Suppress("LongParameterList", "TOO_MANY_PARAMETERS", "UnsafeCallOnNullableType")
    private fun doCreateNew(
        project: Project,
        testSuites: List<TestSuite>,
        allTests: Long,
        files: List<File>,
        username: String,
        sdk: String,
        execCmd: String?,
        batchSizeForAnalyzer: String?,
        testingType: TestingType,
        contestName: String?,
    ): Execution {
        val user = userRepository.findByName(username).orNotFound {
            "Not found user $username"
        }
        val execution = Execution(
            project = project,
            startTime = LocalDateTime.now(),
            endTime = null,
            status = ExecutionStatus.PENDING,
            batchSize = configProperties.initialBatchSize,
            type = testingType,
            version = testSuites.singleVersion(),
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
            user = user,
            execCmd = execCmd,
            batchSizeForAnalyzer = batchSizeForAnalyzer,
            testSuiteSourceName = testSuites.singleSourceName(),
            score = null,
        )

        val savedExecution = executionRepository.save(execution)
        testSuites.map { LnkExecutionTestSuite(savedExecution, it) }.let { lnkExecutionTestSuiteService.saveAll(it) }
        files.map { LnkExecutionFile(savedExecution, it) }.let { lnkExecutionFileRepository.saveAll(it) }
        if (testingType == TestingType.CONTEST_MODE) {
            lnkContestExecutionService.createLink(
                savedExecution, requireNotNull(contestName) {
                    "Requested execution type is ${TestingType.CONTEST_MODE} but no contest name has been specified"
                }
            )
        }
        log.info("Created a new execution id=${savedExecution.id} for project id=${project.id}")
        return savedExecution
    }

    /**
     * Mark [Execution] as [ExecutionStatus.OBSOLETE]
     *
     * @param executionId ID of [Execution]
     */
    @Transactional
    fun markAsObsolete(executionId: Long) {
        findExecution(executionId)
            .orNotFound {
                "Not found execution with id $executionId"
            }
            .let { execution ->
                markAsObsolete(execution)
            }
    }

    /**
     * Mark [Execution] as [ExecutionStatus.OBSOLETE]
     *
     * @param execution
     */
    @Transactional
    fun markAsObsolete(execution: Execution) {
        log.info { "Mark execution with id = ${execution.requiredId()} as obsolete. Additionally delete link to test suites and files" }
        lnkExecutionTestSuiteService.deleteByExecution(execution.requiredId())
        lnkExecutionFileRepository.deleteAll(lnkExecutionFileRepository.findAllByExecution(execution))
        updateExecutionStatus(execution, ExecutionStatus.OBSOLETE)
        // Delete agents, which related to the test suites
        agentStatusService.deleteAgentStatusWithExecutionId(execution.requiredId())
        agentService.deleteAgentByExecutionId(execution.requiredId())
    }

    /**
     * @param execution
     * @return all [File]s are assigned to provided [Execution]
     */
    fun getFiles(execution: Execution): List<File> = execution
        .let { lnkExecutionFileRepository.findAllByExecution(it) }
        .map { it.file }

    /**
     * @param executionId
     */
    fun getFiles(executionId: Long): List<File> = getFiles(getExecution(executionId))

    companion object {
        private fun Collection<TestSuite>.singleSourceName(): String = map { it.source }
            .distinctBy { it.requiredId() }
            .also { sources ->
                require(sources.size == 1) {
                    "Only a single test suites source is allowed for a run, but got: $sources"
                }
            }
            .single()
            .name

        private fun Collection<TestSuite>.singleVersion(): String = map { it.version }
            .distinct()
            .also { versions ->
                require(versions.size == 1) {
                    "Only a single version is supported, but got: $versions"
                }
            }
            .single()
    }
}
