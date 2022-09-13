package com.saveourtool.save.api

import com.saveourtool.save.api.authorization.Authorization
import com.saveourtool.save.api.config.EvaluatedToolProperties
import com.saveourtool.save.api.config.WebClientProperties
import com.saveourtool.save.api.config.toSdk
import com.saveourtool.save.api.utils.getAvailableFilesList
import com.saveourtool.save.api.utils.getExecutionById
import com.saveourtool.save.api.utils.getLatestExecution
import com.saveourtool.save.api.utils.initializeHttpClient
import com.saveourtool.save.api.utils.submitExecution
import com.saveourtool.save.api.utils.uploadAdditionalFile
import com.saveourtool.save.domain.ProjectCoordinates
import com.saveourtool.save.domain.ShortFileInfo
import com.saveourtool.save.entities.RunExecutionRequest
import com.saveourtool.save.execution.ExecutionDto
import com.saveourtool.save.execution.ExecutionStatus
import com.saveourtool.save.execution.TestingType
import com.saveourtool.save.utils.DATABASE_DELIMITER

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import arrow.core.rightIfNotNull
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Accepted
import io.ktor.http.HttpStatusCode.Companion.OK
import okio.Path.Companion.toPath
import org.slf4j.LoggerFactory

import java.io.File
import java.time.LocalDateTime

import kotlinx.coroutines.delay

/**
 * Class, that provides logic for execution submission and result receiving
 */
class SaveCloudClient(
    webClientProperties: WebClientProperties,
    private val evaluatedToolProperties: EvaluatedToolProperties,
    private val testingType: TestingType,
    private val contestName: String?,
    authorization: Authorization,
) {
    private val log = LoggerFactory.getLogger(SaveCloudClient::class.java)
    private var httpClient: HttpClient = initializeHttpClient(authorization, webClientProperties)

    /**
     * Submit execution with provided mode and configuration and receive results
     *
     * @return either the execution result, or an error message.
     * @throws IllegalArgumentException
     */
    @Suppress("UnsafeCallOnNullableType")
    suspend fun start(): Either<String, ExecutionDto> {
        // Calculate FileInfo of additional files, if they are provided
        val additionalFileInfoList = evaluatedToolProperties.additionalFiles?.let {
            processAdditionalFiles(it)
        }

        if (evaluatedToolProperties.additionalFiles != null && additionalFileInfoList == null) {
            return "Unable to parse `additionalFiles`: \"${evaluatedToolProperties.additionalFiles}\"".left()
        }

        val msg = additionalFileInfoList?.let {
            "with additional files: ${additionalFileInfoList.map { it.name }}"
        } ?: {
            "without additional files"
        }
        log.info("Starting submit execution $msg, type: $testingType")

        val executionRequest = submitExecution(additionalFileInfoList, contestName).getOrHandle { httpStatus ->
            return "Failed to submit execution: HTTP $httpStatus".left()
        }

        // Sending requests, which checks current state, until results will be received
        // TODO: in which form do we actually need results?
        val resultExecutionDto = getExecutionResults(executionRequest)
        val errorMessage = "Some errors occurred during execution"
        val resultMsg = resultExecutionDto?.let {
            "Execution with id=${resultExecutionDto.id} is finished with status: ${resultExecutionDto.status}. " +
                    "Passed tests: ${resultExecutionDto.passedTests}, failed tests: ${resultExecutionDto.failedTests}, skipped: ${resultExecutionDto.skippedTests}"
        } ?: errorMessage

        log.info(resultMsg)

        return resultExecutionDto.rightIfNotNull {
            errorMessage
        }
    }

    /**
     * Submit execution
     *
     * @param additionalFiles
     * @return the pair of organization and submitted execution request upon
     *   successful completion, or the HTTP status code if failed.
     */
    private suspend fun submitExecution(
        additionalFiles: List<ShortFileInfo>?,
        contestName: String?,
    ): Either<HttpStatusCode, RunExecutionRequest> {
        val runExecutionRequest = RunExecutionRequest(
            projectCoordinates = ProjectCoordinates(
                organizationName = evaluatedToolProperties.organizationName,
                projectName = evaluatedToolProperties.projectName,
            ),
            testSuiteIds = evaluatedToolProperties.testSuites
                .split(DATABASE_DELIMITER)
                .map { it.toLong() },
            files = additionalFiles?.map { it.toStorageKey() }.orEmpty(),
            sdk = evaluatedToolProperties.sdk.toSdk(),
            execCmd = evaluatedToolProperties.execCmd,
            batchSizeForAnalyzer = evaluatedToolProperties.batchSize,
            testingType = testingType,
            contestName = contestName,
        )
        val response = httpClient.submitExecution(runExecutionRequest)
        val httpStatus = response.status
        if (httpStatus !in arrayOf(OK, Accepted)) {
            log.error("Received HTTP $httpStatus while submitting execution: $runExecutionRequest")
            val responseBody = response.bodyAsText()
            if (responseBody.isNotBlank()) {
                log.error("HTTP response body: $responseBody")
            }
            return httpStatus.left()
        }
        return runExecutionRequest.right()
    }

    /**
     * Get results for current [runExecutionRequest]:
     * sending requests, which checks current state of execution, until it will be finished, or timeout will be reached
     *
     * @param runExecutionRequest
     */
    @Suppress("MagicNumber")
    private suspend fun getExecutionResults(
        runExecutionRequest: RunExecutionRequest,
    ): ExecutionDto? {
        // Execution should be processed in db after submission, so wait little time
        delay(1_000)

        // We suppose, that in this short time (after submission), there weren't any new executions, so we can take the latest one
        val executionId = httpClient.getLatestExecution(runExecutionRequest.projectCoordinates.projectName, runExecutionRequest.projectCoordinates.organizationName).id

        var executionDto = httpClient.getExecutionById(executionId)
        val initialTime = LocalDateTime.now()

        while (executionDto.status == ExecutionStatus.PENDING || executionDto.status == ExecutionStatus.RUNNING) {
            val currTime = LocalDateTime.now()
            if (currTime.minusMinutes(TIMEOUT_MINUTES_FOR_EXECUTION_RESULTS) >= initialTime) {
                log.error("Couldn't get execution result, timeout ${TIMEOUT_MINUTES_FOR_EXECUTION_RESULTS}min is reached!")
                return null
            }
            log.info("Waiting for results of execution with id=$executionId, current state: ${executionDto.status}")
            executionDto = httpClient.getExecutionById(executionId)
            delay(SLEEP_INTERVAL_MILLIS_FOR_EXECUTION_RESULTS)
        }
        return executionDto
    }

    /**
     * Calculate list of FileInfo for additional files, take files from storage,
     * if they are existed or upload them into it
     *
     * @param files
     */
    private suspend fun processAdditionalFiles(
        files: String
    ): List<ShortFileInfo>? {
        val userProvidedAdditionalFiles = files.split(";")
        userProvidedAdditionalFiles.forEach {
            if (!File(it).exists()) {
                log.error("Couldn't find requested additional file $it in user file system!")
                return null
            }
        }

        val availableFilesInCloudStorage = httpClient.getAvailableFilesList()

        val resultFileInfoList: MutableList<ShortFileInfo> = mutableListOf()

        // Try to take files from storage, or upload them if they are absent
        userProvidedAdditionalFiles.forEach { file ->
            val fileFromStorage = availableFilesInCloudStorage.firstOrNull { it.name == file.toPath().name }
            fileFromStorage?.let {
                log.debug("Take existing file ${file.toPath().name} from storage")
                resultFileInfoList.add(fileFromStorage.toShortFileInfo().copy(isExecutable = true))
            } ?: run {
                log.debug("Upload file $file to storage")
                val uploadedFile: ShortFileInfo = httpClient.uploadAdditionalFile(file).copy(isExecutable = true)
                resultFileInfoList.add(uploadedFile)
            }
        }
        return resultFileInfoList
    }

    companion object {
        const val SLEEP_INTERVAL_MILLIS_FOR_EXECUTION_RESULTS = 10_000L
        const val TIMEOUT_MINUTES_FOR_EXECUTION_RESULTS = 5L
    }
}
