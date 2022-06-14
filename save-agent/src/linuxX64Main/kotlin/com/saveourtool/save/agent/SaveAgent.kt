@file:OptIn(ExperimentalCoroutinesApi::class)

package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.logDebugCustom
import com.saveourtool.save.agent.utils.logErrorCustom
import com.saveourtool.save.agent.utils.logInfoCustom
import com.saveourtool.save.agent.utils.readFile
import com.saveourtool.save.agent.utils.sendDataToBackend
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.plugin.Plugin
import com.saveourtool.save.core.result.CountWarnings
import com.saveourtool.save.core.utils.ExecutionResult
import com.saveourtool.save.core.utils.ProcessBuilder
import com.saveourtool.save.domain.TestResultDebugInfo
import com.saveourtool.save.plugins.fix.FixPlugin
import com.saveourtool.save.reporter.Report
import com.saveourtool.save.utils.adjustLocation
import com.saveourtool.save.utils.toTestResultDebugInfo
import com.saveourtool.save.utils.toTestResultStatus

import generated.SAVE_CLOUD_VERSION
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path.Companion.toPath

import kotlin.native.concurrent.AtomicLong
import kotlin.native.concurrent.AtomicReference
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import platform.posix.exit

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
                    is TerminateResponse -> {
                        // Here `coroutineScope` is the topmost scope, which is passed all the way from `fun main()`.
                        // So by cancelling it we can gracefully shut down the whole application.
                        coroutineScope.cancel()
                    }
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
        val executionLogs = ExecutionLogs(config.resolvedId(), readFile(config.logFilePath))
        launchLogSendingJob(executionLogs)
        logDebugCustom("SAVE has completed execution, execution logs:")
        executionLogs.cliLogs.forEach {
            logDebugCustom("[SAVE] $it")
        }
        when (executionResult.code) {
            0 -> if (executionLogs.cliLogs.isEmpty()) {
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
                        config.resolvedId(),
                        testResultStatus,
                        executionStartSeconds.value,
                        currentTime.epochSeconds,
                        unmatched = debugInfo.getCountWarningsAsLong { it.unmatched },
                        matched = debugInfo.getCountWarningsAsLong { it.matched },
                        expected = debugInfo.getCountWarningsAsLong { it.expected },
                        unexpected = debugInfo.getCountWarningsAsLong { it.unexpected },
                    )
                }
            }
        }
    }

    @Suppress("MAGIC_NUMBER", "MagicNumber")
    private fun TestResultDebugInfo.getCountWarningsAsLong(getter: (CountWarnings) -> Int?) = this.debugInfo
        ?.countWarnings
        ?.let { getter(it) }
        ?.toLong()
        ?: 0L

    private fun readExecutionReportFromFile(jsonFile: String) = reportFormat.decodeFromString<List<Report>>(
        readFile(jsonFile).joinToString(separator = "")
    )

    private fun CoroutineScope.launchLogSendingJob(executionLogs: ExecutionLogs) = launch {
        runCatching {
            sendLogs(executionLogs)
        }
            .exceptionOrNull()
            ?.let {
                logErrorCustom("Couldn't send logs, reason: ${it.message}")
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
    private suspend fun sendLogs(executionLogs: ExecutionLogs) = httpClient.post {
        url("${config.orchestratorUrl}/executionLogs")
        contentType(ContentType.Application.Json)
        setBody(executionLogs)
    }

    private suspend fun sendReport(testResultDebugInfo: TestResultDebugInfo) = httpClient.post {
        url("${config.backend.url}/${config.backend.filesEndpoint}/debug-info?agentId=${config.resolvedId()}")
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
            setBody(Heartbeat(config.resolvedId(), state.value, executionProgress, Clock.System.now()))
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
        setBody(AgentVersion(config.resolvedId(), SAVE_CLOUD_VERSION))
    }
}
