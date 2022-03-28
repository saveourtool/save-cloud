package org.cqfn.save.api

import org.cqfn.save.domain.FileInfo
import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto

import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory

import java.io.File
import java.time.LocalDateTime

import org.cqfn.save.entities.Organization
import org.cqfn.save.execution.ExecutionDto
import org.cqfn.save.execution.ExecutionStatus

class AutomaticTestInitializator(
    private val webClientProperties: WebClientProperties,
    private val evaluatedToolProperties: EvaluatedToolProperties
) {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)

    /**
     * @throws IllegalArgumentException
     */
    suspend fun start() {
        val requestUtils = RequestUtils(webClientProperties)

        val additionalFileInfoList = evaluatedToolProperties.additionalFiles?.let {
            processAdditionalFiles(requestUtils, webClientProperties.fileStorage, it)
        }

        if (evaluatedToolProperties.additionalFiles != null && additionalFileInfoList == null) {
            return
        }

        val (organization, executionRequest) = submitExecution(requestUtils, additionalFileInfoList)
        val executionDto = getExecutionResults(requestUtils, executionRequest, organization)
        val resultMsg = executionDto?.let {
            "Execution is finished with status: ${executionDto.status}. " +
                    "Passed tests: ${executionDto.passedTests}, failed tests: ${executionDto.failedTests}, skipped: ${executionDto.skippedTests}"
        } ?: "Some errors occured during execution"

        log.info(resultMsg)
    }

    /**
     * @param requestUtils
     * @param additionalFiles
     */
    suspend fun submitExecution(requestUtils: RequestUtils, additionalFiles: List<FileInfo>?): Pair<Organization, ExecutionRequest> {
        val (organization, executionRequest) = buildExecutionRequest(requestUtils, additionalFiles)
        requestUtils.submitExecution(executionRequest, additionalFiles)
        return organization to executionRequest
    }

    private suspend fun buildExecutionRequest(
        requestUtils: RequestUtils,
        additionalFiles: List<FileInfo>?
    ): Pair<Organization, ExecutionRequest> {
        val msg = additionalFiles?.let {
            "with additional files: ${additionalFiles.map { it.name }}"
        } ?: {
            "without additional files"
        }
        log.info("Starting submit execution $msg")

        val organization = requestUtils.getOrganizationByName(evaluatedToolProperties.organizationName)
        val organizationId = organization.id!!
        val project = requestUtils.getProjectByNameAndOrganizationId(evaluatedToolProperties.projectName, organizationId)

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

    private suspend fun getExecutionResults(requestUtils: RequestUtils, executionRequest: ExecutionRequest, organization: Organization): ExecutionDto? {
        // execution should be processed in db, so wait little time
        Thread.sleep(1_000)

        // we suppose, that in this short time (after submission), there weren't any new executions, so we can take the latest one
        val executionId = requestUtils.getLatestExecution(executionRequest.project.name, organization.id!!).id

        var executionDto = requestUtils.getExecutionById(executionId)
        val initialTime = LocalDateTime.now()

        while (executionDto.status == ExecutionStatus.PENDING || executionDto.status == ExecutionStatus.RUNNING) {
            val currTime = LocalDateTime.now()
            if (currTime.minusMinutes(TIMEOUT_FOR_EXECUTION_RESULTS) >= initialTime) {
                log.error("Couldn't get execution result, timeout ${TIMEOUT_FOR_EXECUTION_RESULTS}min is reached!")
                return null
            }
            log.debug("Waiting for results of execution with id=${executionId}, current state: ${executionDto.status}")
            executionDto = requestUtils.getExecutionById(executionId)
            Thread.sleep(SLEEP_INTERVAL_FOR_EXECUTION_RESULTS)
        }
        return executionDto
    }

    suspend fun submitExecutionStandardMode() {
        TODO("Not yet implemented")
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
                log.debug("Take existing file $filePathInStorage from storage")
                if (!File(filePathInStorage).exists()) {
                    log.error("Couldn't find additional file $filePathInStorage in cloud storage!")
                    return null
                }
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
        const val TIMEOUT_FOR_EXECUTION_RESULTS = 5L
        const val SLEEP_INTERVAL_FOR_EXECUTION_RESULTS = 10_000L
    }
}
