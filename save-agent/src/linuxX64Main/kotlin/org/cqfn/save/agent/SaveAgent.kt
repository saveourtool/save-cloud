@file:OptIn(ExperimentalCoroutinesApi::class)

package org.cqfn.save.agent

import org.cqfn.save.agent.utils.logDebugCustom
import org.cqfn.save.agent.utils.logErrorCustom
import org.cqfn.save.agent.utils.logInfoCustom
import org.cqfn.save.agent.utils.readFile
import org.cqfn.save.agent.utils.sendDataToBackend
import org.cqfn.save.core.logging.describe
import org.cqfn.save.core.plugin.Plugin
import org.cqfn.save.core.utils.ExecutionResult
import org.cqfn.save.core.utils.ProcessBuilder
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.plugins.fix.FixPlugin
import org.cqfn.save.reporter.Report
import org.cqfn.save.utils.adjustLocation
import org.cqfn.save.utils.toTestResultDebugInfo
import org.cqfn.save.utils.toTestResultStatus

import generated.SAVE_CLOUD_VERSION
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

import kotlin.native.concurrent.AtomicLong
import kotlin.native.concurrent.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * A main class for SAVE Agent
 * @property config
 */
class SaveAgent(internal val config: AgentConfiguration,
                private val httpClient: HttpClient
) {
    /**
     * The current [AgentState] of this agent
     */
    val state = AtomicReference(AgentState.STARTING)

    // fixme (limitation of old MM): can't use atomic reference to Instant here, because when using `Clock.System.now()` as an assigned value
    // Kotlin throws `kotlin.native.concurrent.InvalidMutabilityException: mutation attempt of frozen kotlinx.datetime.Instant...`
    private val executionStartSeconds = AtomicLong()
    private var saveProcessJob: AtomicReference<Job?> = AtomicReference(null)
    private val backgroundContext = newSingleThreadContext("background")
    private val saveProcessContext = newSingleThreadContext("save-process")
    private val reportFormat = Json {
        serializersModule = SerializersModule {
            polymorphic(Plugin.TestFiles::class) {
                subclass(Plugin.Test::class)
                subclass(FixPlugin.FixTestFiles::class)
            }
        }
    }

    /**
     * Starts save-agent and required jobs in the background and then immediately returns
     *
     * @param coroutineScope a [CoroutineScope] to launch other jobs
     * @return a descriptor of the main coroutine job
     */
    fun start(coroutineScope: CoroutineScope): Job {
        logInfoCustom("Starting agent")
        coroutineScope.launch(backgroundContext) {
            sendDataToBackend { saveAdditionalData() }
        }
        return coroutineScope.launch { startHeartbeats(this) }
    }

    @Suppress("WHEN_WITHOUT_ELSE")  // when with sealed class
    private suspend fun startHeartbeats(coroutineScope: CoroutineScope) {
        logInfoCustom("Scheduling heartbeats")
        while (true) {
            val response = runCatching {
                // TODO: get execution progress here. However, with current implementation JSON report won't be valid until all tests are finished.
                sendHeartbeat(ExecutionProgress(0))
            }
            if (response.isSuccess) {
                when (val heartbeatResponse = response.getOrThrow().also {
                    logDebugCustom("Got heartbeat response $it")
                }) {
                    is NewJobResponse -> coroutineScope.launch {
                        maybeStartSaveProcess(heartbeatResponse.cliArgs)
                    }
                    is WaitResponse -> state.value = AgentState.IDLE
                    is ContinueResponse -> Unit  // do nothing
                }
            } else {
                logErrorCustom("Exception during heartbeat: ${response.exceptionOrNull()?.message}")
                response.exceptionOrNull()?.printStackTrace()
            }
            // todo: start waiting after request was sent, not after response?
            logInfoCustom("Waiting for ${config.heartbeat.intervalMillis} ms")
            delay(config.heartbeat.intervalMillis)
        }
    }

    private fun CoroutineScope.maybeStartSaveProcess(cliArgs: String) {
        if (saveProcessJob.value?.isCompleted == false) {
            logErrorCustom("Shouldn't start new process when there is the previous running")
        } else {
            saveProcessJob.value = launch(saveProcessContext) {
                runCatching {
                    // new job received from Orchestrator, spawning SAVE CLI process
                    startSaveProcess(cliArgs)
                }
                    .exceptionOrNull()
                    ?.let {
                        state.value = AgentState.CLI_FAILED
                        logErrorCustom("Error executing SAVE: ${it.describe()}\n" + it.stackTraceToString())
                    }
            }
        }
    }

    /**
     * @param cliArgs arguments for SAVE process
     * @return Unit
     */
    internal fun CoroutineScope.startSaveProcess(cliArgs: String) {
        // blocking execution of OS process
        state.value = AgentState.BUSY
        executionStartSeconds.value = Clock.System.now().epochSeconds
        logInfoCustom("Starting SAVE with provided args $cliArgs")
        val executionResult = runSave(cliArgs)
        logInfoCustom("SAVE has completed execution with status ${executionResult.code}")
        val saveCliLogFile = config.logFilePath
        val saveCliLogs = readFile(saveCliLogFile)
        launchLogSendingJob(saveCliLogFile)
        logDebugCustom("SAVE has completed execution, execution logs:")
        saveCliLogs.forEach {
            logDebugCustom("[SAVE] $it")
        }
        when (executionResult.code) {
            0 -> if (saveCliLogs.isEmpty()) {
                state.value = AgentState.CLI_FAILED
            } else {
                handleSuccessfulExit()
                state.value = AgentState.FINISHED
            }
            else -> {
                logErrorCustom("SAVE has exited abnormally with status ${executionResult.code}")
                state.value = AgentState.CLI_FAILED
            }
        }
    }

    @Suppress("MagicNumber")
    private fun runSave(cliArgs: String): ExecutionResult = ProcessBuilder(true, FileSystem.SYSTEM)
        .exec(
            config.cliCommand.let {
                "$it $cliArgs"
            } + " --report-type json --result-output file --log all",
            "",
            config.logFilePath.toPath(),
            1_000_000L
        )

    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    private fun CoroutineScope.readExecutionResults(jsonFile: String): List<TestExecutionDto> {
        val currentTime = Clock.System.now()
        val reports: List<Report> = readExecutionReportFromFile(jsonFile)
        return reports.flatMap { report ->
            report.pluginExecutions.flatMap { pluginExecution ->
                pluginExecution.testResults.map { tr ->
                    val debugInfo = tr.toTestResultDebugInfo(report.testSuite, pluginExecution.plugin)
                    launch {
                        logDebugCustom("Posting debug info for test ${debugInfo.testResultLocation}")
                        sendDataToBackend {
                            sendReport(debugInfo)
                        }
                    }
                    val testResultStatus = tr.status.toTestResultStatus()
                    TestExecutionDto(
                        adjustLocation(tr.resources.test.toString()),
                        pluginExecution.plugin,
                        config.id,
                        testResultStatus,
                        executionStartSeconds.value,
                        currentTime.epochSeconds,
                        missingWarnings = debugInfo.debugInfo?.countWarnings?.missing,
                        matchedWarnings = debugInfo.debugInfo?.countWarnings?.match,
                    )
                }
            }
        }
    }

    private fun readExecutionReportFromFile(jsonFile: String) = reportFormat.decodeFromString<List<Report>>(
        readFile(jsonFile).joinToString(separator = "")
    )

    private fun CoroutineScope.launchLogSendingJob(saveCliLogFile: String): Job {
        val byteArray = FileSystem.SYSTEM.source(saveCliLogFile.toPath()).buffer().readByteArray()
        return launch {
            runCatching {
                sendLogs(byteArray)
            }
                .exceptionOrNull()
                ?.let {
                    logErrorCustom("Couldn't send logs, reason: ${it.message}")
                }
        }
    }

    private fun CoroutineScope.handleSuccessfulExit() {
        val jsonReport = "save.out.json"
        val testExecutionDtos = runCatching {
            readExecutionResults(jsonReport)
        }
        if (testExecutionDtos.isFailure) {
            logErrorCustom("Couldn't read execution results from JSON report, reason: ${testExecutionDtos.exceptionOrNull()?.describe()}" +
                    "\n${testExecutionDtos.exceptionOrNull()?.stackTraceToString()}"
            )
        } else {
            launch {
                sendDataToBackend {
                    postExecutionData(testExecutionDtos.getOrThrow())
                }
            }
        }
    }

    /**
     * @param executionLogs logs of CLI execution progress that will be sent in a message
     */
    @OptIn(InternalAPI::class)
    private suspend fun sendLogs(byteArray: ByteArray): HttpResponse =
            httpClient.post {
                url("${config.orchestratorUrl}/executionLogs")
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "executionLogs",
                        byteArray,
                        Headers.build {
                            append(HttpHeaders.ContentType, ContentType.MultiPart.FormData)
                            append(HttpHeaders.ContentDisposition, "filename=${config.id}")
                        }
                    )
                }))
            }

    private suspend fun sendReport(testResultDebugInfo: TestResultDebugInfo) = httpClient.post {
        url("${config.backend.url}/${config.backend.filesEndpoint}/debug-info?agentId=${config.id}")
        contentType(ContentType.Application.Json)
        setBody(testResultDebugInfo)
    }

    /**
     * @param executionProgress execution progress that will be sent in a heartbeat message
     * @return a [HeartbeatResponse] from Orchestrator
     */
    internal suspend fun sendHeartbeat(executionProgress: ExecutionProgress): HeartbeatResponse {
        logDebugCustom("Sending heartbeat to ${config.orchestratorUrl}")
        // if current state is IDLE or FINISHED, should accept new jobs as a response
        return httpClient.post {
            url("${config.orchestratorUrl}/heartbeat")
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(Heartbeat(config.id, state.value, executionProgress, Clock.System.now()))
        }
            .body()
    }

    private suspend fun postExecutionData(testExecutionDtos: List<TestExecutionDto>) = httpClient.post {
        logInfoCustom("Posting execution data to backend, ${testExecutionDtos.size} test executions")
        url("${config.backend.url}/${config.backend.executionDataEndpoint}")
        contentType(ContentType.Application.Json)
        setBody(testExecutionDtos)
    }

    private suspend fun saveAdditionalData() = httpClient.post {
        logInfoCustom("Posting additional data to backend")
        url("${config.backend.url}/${config.backend.additionalDataEndpoint}")
        contentType(ContentType.Application.Json)
        setBody(AgentVersion(config.id, SAVE_CLOUD_VERSION))
    }
}
