package org.cqfn.save.agent

import org.cqfn.save.agent.utils.readFile
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logError
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.utils.ExecutionResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.domain.TestResultStatus

import generated.SAVE_CLOUD_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

import kotlin.native.concurrent.AtomicReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.result.Crash
import org.cqfn.save.core.result.Fail
import org.cqfn.save.core.result.Ignored
import org.cqfn.save.core.result.Pass
import org.cqfn.save.reporter.Report

/**
 * A main class for SAVE Agent
 */
@OptIn(ExperimentalFileSystem::class)
class SaveAgent(private val config: AgentConfiguration,
                private val httpClient: HttpClient = HttpClient {
                    install(JsonFeature) {
                        serializer = KotlinxSerializer(Json {
                            serializersModule = SerializersModule {
                                // for some reason for K/N it's needed explicitly, at least for ktor 1.5.1, kotlin 1.4.21
                                contextual(HeartbeatResponse::class, HeartbeatResponse.serializer())
                            }
                        })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = config.requestTimeoutMillis
                    }
                }
) {
    private val logFilePath = "logs.txt"

    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.STARTING)
    private var saveProcessJob: Job? = null

    /**
     * @return Unit
     */
    suspend fun start() = coroutineScope {
        logInfo("Starting agent")
        val heartbeatsJob = launch { startHeartbeats() }
        heartbeatsJob.join()
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // when with sealed class
    private suspend fun startHeartbeats() = coroutineScope {
        logInfo("Scheduling heartbeats")
        sendDataToBackend { saveAdditionalData() }
        while (true) {
            val response = runCatching {
                // TODO: get execution progress here. However, with current implementation JSON report won't be valid
                //  until all tests are finished.
                sendHeartbeat(ExecutionProgress(0))
            }
            if (response.isSuccess) {
                when (val heartbeatResponse = response.getOrNull().also {
                    logDebug("Got heartbeat response $it")
                }) {
                    is NewJobResponse -> maybeStartSaveProcess(heartbeatResponse.cliArgs)
                    WaitResponse -> state.value = AgentState.IDLE
                    ContinueResponse -> Unit  // do nothing
                }
            } else {
                logError("Exception during heartbeat: ${response.exceptionOrNull()?.message}")
            }
            // todo: start waiting after request was sent, not after response?
            logInfo("Waiting for ${config.heartbeat.intervalMillis} ms")
            delay(config.heartbeat.intervalMillis)
        }
    }

    private suspend fun maybeStartSaveProcess(cliArgs: String) = coroutineScope {
        if (saveProcessJob?.isCompleted == false) {
            logError("Shouldn't start new process when there is the previous running")
        } else {
            saveProcessJob = launch {
                runCatching {
                    // new job received from Orchestrator, spawning SAVE CLI process
                    startSaveProcess(cliArgs)
                }
                    .exceptionOrNull()
                    ?.let {
                        logError("Error executing SAVE: ${it.describe()}")
                    }
            }
        }
    }

    /**
     * @param cliArgs arguments for SAVE process
     * @return Unit
     */
    @Suppress("MAGIC_NUMBER")  // todo: unsuppress when mocked data is substituted by actual
    internal suspend fun startSaveProcess(cliArgs: String) = coroutineScope {
        // blocking execution of OS process
        state.value = AgentState.BUSY
        logInfo("Starting SAVE with provided args $cliArgs")
        val executionResult = runSave(cliArgs)
        logInfo("SAVE has completed execution with status ${executionResult.code}")
//        logDebug("Executed SAVE, here is stdout: ${executionResult.stdout}")
//        logDebug("Executed SAVE, here is stderr: ${executionResult.stderr}")
        val executionLogs = ExecutionLogs(config.id, readFile(logFilePath))
        val logsSending = launch {
            runCatching {
                sendLogs(executionLogs)
            }
                .exceptionOrNull()
                ?.let {
                    logError("Couldn't send logs, reason: ${it.message}")
                }
        }
        when (executionResult.code) {
            0 -> {
                if (executionLogs.cliLogs.isEmpty()) {
                    state.value = AgentState.CLI_FAILED
                    return@coroutineScope
                }
                val jsonReport = "save.out.json"
                val testExecutionDtos = runCatching {
                    readExecutionResults(jsonReport)
                }
                if (testExecutionDtos.isFailure) {
                    logError("Couldn't read execution results from JSON report, reason: ${testExecutionDtos.exceptionOrNull()?.describe()}")
                } else {
                    sendDataToBackend {
                        postExecutionData(testExecutionDtos.getOrThrow())
                    }
                }
                state.value = AgentState.FINISHED
            }
            else -> {
                logError("SAVE has exited abnormally with status ${executionResult.code}")
                state.value = AgentState.CLI_FAILED
            }
        }
        logsSending.join()
    }

    private fun runSave(cliArgs: String): ExecutionResult = ProcessBuilder(true)
        .exec(config.cliCommand.let { if (cliArgs.isNotEmpty()) "$it $cliArgs" else it }, logFilePath.toPath())

    private fun readExecutionResults(jsonFile: String): List<TestExecutionDto> {
        // todo: startTime
        val currentTime = Clock.System.now()
        val reports = Json.decodeFromString<List<Report>>(
            readFile(jsonFile).joinToString(separator = "")
        )
        return reports.flatMap { report ->
            report.pluginExecutions.flatMap { pluginExecution ->
                pluginExecution.testResults.map {
                    val testResultStatus = when (it.status) {
                        is Pass -> TestResultStatus.PASSED
                        is Fail -> TestResultStatus.FAILED
                        is Ignored -> TestResultStatus.IGNORED
                        is Crash -> TestResultStatus.TEST_ERROR
                    }
                    TestExecutionDto(it.resources.first().name, config.id, testResultStatus, currentTime, currentTime)
                }
            }
        }
    }

    /**
     * @param executionLogs logs of CLI execution progress that will be sent in a message
     */
    private suspend fun sendLogs(executionLogs: ExecutionLogs) = httpClient.post<HttpResponse> {
        url("${config.orchestratorUrl}/executionLogs")
        contentType(ContentType.Application.Json)
        body = executionLogs
    }

    /**
     * @param executionProgress execution progress that will be sent in a heartbeat message
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(executionProgress: ExecutionProgress): HeartbeatResponse {
        // log.trace("Sending heartbeat to $orchestratorUrl")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post("${config.orchestratorUrl}/heartbeat") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = Heartbeat(config.id, state.value, executionProgress)
        }
    }

    /**
     * Attempt to send execution data to backend, will retry several times, while increasing delay 2 times on each iteration.
     */
    private suspend fun sendDataToBackend(
        requestToBackend: suspend () -> HttpResponse
    ) = coroutineScope {
        var retryInterval = config.executionDataInitialRetryMillis
        repeat(config.executionDataRetryAttempts) { attempt ->
            val result = runCatching {
                requestToBackend()
            }
            if (result.isSuccess && result.getOrNull()?.status == HttpStatusCode.OK) {
                return@coroutineScope
            } else {
                val reason = if (result.isSuccess && result.getOrNull()?.status != HttpStatusCode.OK) {
                    state.value = AgentState.BACKEND_FAILURE
                    "Backend returned status ${result.getOrNull()?.status}"
                } else {
                    state.value = AgentState.BACKEND_UNREACHABLE
                    "Backend is unreachable, ${result.exceptionOrNull()?.message}"
                }
                logError("Cannot post data (x${attempt + 1}), will retry in $retryInterval second. Reason: $reason")
                delay(retryInterval)
                retryInterval *= 2
            }
        }
    }

    private suspend fun postExecutionData(testExecutionDtos: List<TestExecutionDto>) = httpClient.post<HttpResponse> {
        logInfo("Posting execution data to backend, ${testExecutionDtos.size} test executions")
        url("${config.backendUrl}/executionData")
        contentType(ContentType.Application.Json)
        body = testExecutionDtos
    }

    private suspend fun saveAdditionalData() = httpClient.post<HttpResponse> {
        logInfo("Posting additional data to backend")
        url("${config.backendUrl}/saveAgentVersion")
        contentType(ContentType.Application.Json)
        body = AgentVersion(config.id, SAVE_CLOUD_VERSION)
    }
}
