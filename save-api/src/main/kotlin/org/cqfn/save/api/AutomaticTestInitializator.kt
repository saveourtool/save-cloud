package org.cqfn.save.api

import org.cqfn.save.domain.Jdk
import org.cqfn.save.entities.ExecutionRequest
import org.cqfn.save.entities.GitDto
import org.cqfn.save.utils.LocalDateTimeSerializer


import org.slf4j.LoggerFactory

import java.lang.IllegalArgumentException
import java.time.LocalDateTime

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import okio.Path.Companion.toPath
import org.cqfn.save.domain.FileInfo
import java.io.File

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

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
        val executionRequest = buildExecutionRequest(requestUtils, additionalFiles)

        requestUtils.submitExecution(executionRequest, additionalFiles)
    }

    private suspend fun buildExecutionRequest(requestUtils: RequestUtils, additionalFiles: List<FileInfo>?): ExecutionRequest {
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

        // Actually it's just a stub, executionId will be calculated at the server side
        val executionId = 1L

        val executionRequest = ExecutionRequest(
            project = project,
            gitDto = gitDto,
            testRootPath = evaluatedToolProperties.testRootPath,
            sdk = Jdk("11"),
            executionId = executionId,
        )
        return executionRequest
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

        val availableFilesInCloudStorage = requestUtils.getAvaliableFilesList()

        val resultFileInfoList: MutableList<FileInfo> = mutableListOf()

        // Try to take files from storage, or upload them if they are absent
        userProvidedAdditionalFiles.forEach { file ->
            val fileFromStorage = availableFilesInCloudStorage.firstOrNull { it.name == file.toPath().name }
            fileFromStorage?.let {
                val filePathInStorage = "${fileStorage}/${fileFromStorage.uploadedMillis}/${fileFromStorage.name}"
                log.info("Take existing file $filePathInStorage from storage")
                if (!File(filePathInStorage).exists()) {
                    log.error("Couldn't find additional file $filePathInStorage in cloud storage!")
                    return null
                }
                resultFileInfoList.add(fileFromStorage)
            } ?: run {
                log.info("Upload file $file to storage")
                val uploadedFile: FileInfo = requestUtils.uploadAdditionalFile(file)
                resultFileInfoList.add(uploadedFile)
            }
        }
        return resultFileInfoList
    }
}
