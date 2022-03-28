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

        submitExecution(requestUtils, additionalFileInfoList)
    }

    /**
     * @param requestUtils
     * @param additionalFiles
     */
    suspend fun submitExecution(requestUtils: RequestUtils, additionalFiles: List<FileInfo>?) {
        val (organization, executionRequest) = buildExecutionRequest(requestUtils, additionalFiles)
        requestUtils.submitExecution(executionRequest, additionalFiles)

        // TODO sleep

        // we suppose, that in this short time (after submission), there weren't any new executions, so we can take the latest one
        val executionId = requestUtils.getLatestExecution(executionRequest.project.name, organization.id!!).id

        var execution = requestUtils.getExecutionById(executionId)

        val timeout = 5L
        val sleepInterval = 10_000L
        val initialTime = LocalDateTime.now()

        while (execution.status == ExecutionStatus.PENDING || execution.status == ExecutionStatus.RUNNING) {
            val currTime = LocalDateTime.now()
            if (currTime.minusMinutes(timeout) >= initialTime) {
                log.error("Couldn't get execution result, timeout $timeout minutes is reached!")
                return
            }
            log.debug("Waiting for execution results for execution with id=${executionId}, current state: ${execution.status}")
            execution = requestUtils.getExecutionById(executionId)
            Thread.sleep(sleepInterval)
        }
        println("=======================-----------------================")
        println("ID ${execution.id} Status ${execution.status}")
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
}
