/**
 * Main entrypoint for SAVE Agent
 */

package com.saveourtool.save.agent

import com.saveourtool.save.agent.utils.ktorLogger
import com.saveourtool.save.agent.utils.logDebugCustom
import com.saveourtool.save.agent.utils.logInfoCustom
import com.saveourtool.save.agent.utils.markAsExecutable
import com.saveourtool.save.agent.utils.optionalEnv
import com.saveourtool.save.agent.utils.readProperties
import com.saveourtool.save.core.config.LogType
import com.saveourtool.save.core.logging.describe
import com.saveourtool.save.core.logging.logType

import generated.SAVE_CLOUD_VERSION
import generated.SAVE_CORE_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.*

import kotlinx.cinterop.staticCFunction
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

internal val json = Json {
    serializersModule = SerializersModule {
        contextual(HeartbeatResponse::class, PolymorphicSerializer(HeartbeatResponse::class))
        polymorphic(HeartbeatResponse::class) {
            subclass(NewJobResponse::class)
            subclass(ContinueResponse::class)
            subclass(WaitResponse::class)
            subclass(TerminateResponse::class)
        }
    }
}

internal val fs = FileSystem.SYSTEM

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val config: AgentConfiguration = Properties.decodeFromStringMap<AgentConfiguration>(
        readProperties("agent.properties")
    ).updateFromEnv()
    logType.set(if (config.debug) LogType.ALL else LogType.WARN)
    logDebugCustom("Instantiating save-agent version $SAVE_CLOUD_VERSION with config $config")

    "save-$SAVE_CORE_VERSION-linuxX64.kexe".toPath().markAsExecutable()

    signal(SIGTERM, staticCFunction<Int, Unit> {
        logInfoCustom("Agent is shutting down because SIGTERM has been received")
        exit(1)
    })

    val httpClient = configureHttpClient(config)

    runBlocking {
        // Launch in a new scope, because we cancel the scope on graceful termination,
        // and `BlockingCoroutine` shouldn't be cancelled.
        launch {
            val saveAgent = SaveAgent(config, httpClient, coroutineScope = this)

            val mainJob = saveAgent.start()
            mainJob.join()
        }
    }
    logInfoCustom("Agent is shutting down")
}

@Suppress("FLOAT_IN_ACCURATE_CALCULATIONS", "MagicNumber")
private fun configureHttpClient(agentConfiguration: AgentConfiguration) = HttpClient {
    install(ContentNegotiation) {
        json(json = json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = agentConfiguration.requestTimeoutMillis
    }
    install(HttpRequestRetry) {
        retryOnException(maxRetries = agentConfiguration.retry.attempts)
        retryOnServerErrors(maxRetries = agentConfiguration.retry.attempts)
        exponentialDelay(base = agentConfiguration.retry.initialRetryMillis / 1000.0)
        modifyRequest {
            if (retryCount > 1) {
                val reason = response?.status ?: cause?.describe() ?: "Unknown reason"
                logDebugCustom("Retrying request: attempt #$retryCount, reason: $reason")
            }
        }
    }
    install(Logging) {
        logger = ktorLogger
        level = if (agentConfiguration.debug) LogLevel.ALL else LogLevel.INFO
    }
}
