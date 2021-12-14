/**
 * Main entrypoint for SAVE Agent
 */

package org.cqfn.save.agent

import org.cqfn.save.agent.utils.readProperties
import org.cqfn.save.core.config.LogType
import org.cqfn.save.core.logging.logDebug
import org.cqfn.save.core.logging.logInfo
import org.cqfn.save.core.logging.logType

import generated.SAVE_CLOUD_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import platform.posix.SIGTERM
import platform.posix.exit
import platform.posix.signal

import kotlinx.cinterop.staticCFunction
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
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val config: AgentConfiguration = Properties.decodeFromStringMap(
        readProperties("agent.properties")
    )
    logType.set(if (config.debug) LogType.ALL else LogType.WARN)
    logDebug("Instantiating save-agent version $SAVE_CLOUD_VERSION with config $config")

    signal(SIGTERM, staticCFunction<Int, Unit> {
        logInfo("Agent is shutting down because SIGTERM has been received")
        exit(1)
    })

    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.requestTimeoutMillis
        }
    }
    val saveAgent = SaveAgent(config, httpClient)
    runBlocking {
        saveAgent.start()
    }
    logInfo("Agent is shutting down")
}
