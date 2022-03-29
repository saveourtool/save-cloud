package org.cqfn.save.api

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.ExecutionRequestBase
import org.cqfn.save.entities.ExecutionRequestForStandardSuites
import org.cqfn.save.entities.GitDto
import org.cqfn.save.entities.Organization
import org.cqfn.save.entities.Project
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus
import org.cqfn.save.execution.ExecutionType

import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory

import java.io.File
import java.time.LocalDateTime

class AutomaticTestInitializator(
    private val webClientProperties: WebClientProperties,
    private val evaluatedToolProperties: EvaluatedToolProperties
) {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)

    /**
     * @param executionType
     * @throws IllegalArgumentException
     */
    suspend fun start(executionType: ExecutionType) {
        val requestUtils = RequestUtils(webClientProperties)

        val additionalFileInfoList = evaluatedToolProperties.additionalFiles?.let {
            processAdditionalFiles(requestUtils, webClientProperties.fileStorage, it)
        }

        if (evaluatedToolProperties.additionalFiles != null && additionalFileInfoList == null) {
            return
        }

        val msg = additionalFileInfoList?.let {
            "with additional files: ${additionalFileInfoList.map { it.name }}"
        } ?: {
            "without additional files"
        }
        log.info("Starting submit execution $msg, type: $executionType")

        val result = submitExecution(executionType, requestUtils, additionalFileInfoList) ?: return

        val (organization, executionRequest) = result
        val executionDto = getExecutionResults(requestUtils, executionRequest, organization)
        val resultMsg = executionDto?.let {
            "Execution is finished with status: ${executionDto.status}. " +
                    "Passed tests: ${executionDto.passedTests}, failed tests: ${executionDto.failedTests}, skipped: ${executionDto.skippedTests}"
        } ?: "Some errors occurred during execution"

        log.info(resultMsg)
    }

    /**
     * @param requestUtils
     * @param additionalFiles
     */
    private suspend fun submitExecution(
        executionType: ExecutionType,
        requestUtils: RequestUtils,
        additionalFiles: List<FileInfo>?
    ): Pair<Organization, ExecutionRequestBase>? {
        val (organization, executionRequest) = if (executionType == ExecutionType.GIT) {
            buildExecutionRequest(requestUtils)
        } else {
            val userProvidedTestSuites = processTestSuites(requestUtils) ?: return null
            buildExecutionRequestForStandardSuites(requestUtils, userProvidedTestSuites)
        }
        requestUtils.submitExecution(executionType, executionRequest, additionalFiles)
        return organization to executionRequest
    }

    private suspend fun buildExecutionRequest(
        requestUtils: RequestUtils,
    ): Pair<Organization, ExecutionRequest> {
        val (organization, project) = getOrganizationAndProject(requestUtils)

        val gitDto = GitDto(
            url = evaluatedToolProperties.gitUrl,
            username = evaluatedToolProperties.gitUserName,
            password = evaluatedToolProperties.gitPassword,
            branch = evaluatedToolProperties.branch,
            hash = evaluatedToolProperties.commitHash
        )

        // executionId will be calculated at the server side
        val executionId = null

        return organization to ExecutionRequest(
            project = project,
            gitDto = gitDto,
            testRootPath = evaluatedToolProperties.testRootPath,
            sdk = Jdk("11"),
            executionId = executionId,
        )
    }

    private suspend fun buildExecutionRequestForStandardSuites(
        requestUtils: RequestUtils,
        userProvidedTestSuites: List<String>
    ): Pair<Organization, ExecutionRequestForStandardSuites> {
        val (organization, project) = getOrganizationAndProject(requestUtils)

        return organization to ExecutionRequestForStandardSuites(
            project = project,
            testsSuites = userProvidedTestSuites,
            sdk = Jdk("11"),
            execCmd = evaluatedToolProperties.execCmd,
            batchSizeForAnalyzer = evaluatedToolProperties.batchSize
        )
    }

    private suspend fun processTestSuites(requestUtils: RequestUtils): List<String>? {
        val userProvidedTestSuites = evaluatedToolProperties.testSuites.split(";")
        if (userProvidedTestSuites.isEmpty()) {
            log.error("Set of test suites couldn't be empty in standard mode!")
            return null
        }

        val existingTestSuites = requestUtils.getStandardTestSuites().map { it.name }

        userProvidedTestSuites.forEach {
            if (it !in existingTestSuites) {
                log.error("Incorrect standard test suite $it, available are $existingTestSuites")
                return null
            }
        }
        return userProvidedTestSuites
    }

    private suspend fun getOrganizationAndProject(requestUtils: RequestUtils): Pair<Organization, Project> {
        val organization = requestUtils.getOrganizationByName(evaluatedToolProperties.organizationName)
        val project = requestUtils.getProjectByNameAndOrganizationId(evaluatedToolProperties.projectName, organization.id!!)
        return organization to project
    }

    private suspend fun getExecutionResults(requestUtils: RequestUtils, executionRequest: ExecutionRequestBase, organization: Organization): ExecutionDto? {
        // Execution should be processed in db after submission, so wait little time
        Thread.sleep(1_000)

        // We suppose, that in this short time (after submission), there weren't any new executions, so we can take the latest one
        val executionId = requestUtils.getLatestExecution(executionRequest.project.name, organization.id!!).id

        var executionDto = requestUtils.getExecutionById(executionId)
        val initialTime = LocalDateTime.now()

        while (executionDto.status == ExecutionStatus.PENDING || executionDto.status == ExecutionStatus.RUNNING) {
            val currTime = LocalDateTime.now()
            if (currTime.minusMinutes(TIMEOUT_FOR_EXECUTION_RESULTS) >= initialTime) {
                log.error("Couldn't get execution result, timeout ${TIMEOUT_FOR_EXECUTION_RESULTS}min is reached!")
                return null
            }
            log.info("Waiting for results of execution with id=$executionId, current state: ${executionDto.status}")
            executionDto = requestUtils.getExecutionById(executionId)
            Thread.sleep(SLEEP_INTERVAL_FOR_EXECUTION_RESULTS)
        }
        return executionDto
    }

    private suspend fun processAdditionalFiles(requestUtils: RequestUtils, fileStorage: String, files: String): List<FileInfo>? {
        val userProvidedAdditionalFiles = files.split(";")
        userProvidedAdditionalFiles.forEach {
            if (!File(it).exists()) {
                log.error("Couldn't find requested additional file $it in user file system!")
                return null
            }
        }

        val availableFilesInCloudStorage = requestUtils.getAvailableFilesList()

        val resultFileInfoList: MutableList<FileInfo> = mutableListOf()

        // Try to take files from storage, or upload them if they are absent
        userProvidedAdditionalFiles.forEach { file ->
            val fileFromStorage = availableFilesInCloudStorage.firstOrNull { it.name == file.toPath().name }
            fileFromStorage?.let {
                val filePathInStorage = "$fileStorage/${fileFromStorage.uploadedMillis}/${fileFromStorage.name}"
                if (!File(filePathInStorage).exists()) {
                    log.error("Couldn't find additional file $file in cloud storage!")
                    return null
                }
                log.debug("Take existing file $file from storage")
                resultFileInfoList.add(fileFromStorage)
            } ?: run {
                log.debug("Upload file $file to storage")
                val uploadedFile: FileInfo = requestUtils.uploadAdditionalFile(file)
                resultFileInfoList.add(uploadedFile)
            }
        }
        return resultFileInfoList
    }

    companion object {
        const val SLEEP_INTERVAL_FOR_EXECUTION_RESULTS = 10_000L
        const val TIMEOUT_FOR_EXECUTION_RESULTS = 5L
    }
}
