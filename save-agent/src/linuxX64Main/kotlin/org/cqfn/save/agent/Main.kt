/**
 * Main entrypoint for SAVE Agent
 */

package org.cqfn.save.agent

import generated.SAVE_CLOUD_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import org.cqfn.save.agent.utils.readProperties
import org.cqfn.save.core.logging.isDebugEnabled
import org.cqfn.save.core.logging.logDebug

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val config: AgentConfiguration = Properties.decodeFromStringMap(
        readProperties("agent.properties")
    )
    isDebugEnabled = config.debug
    logDebug("Instantiating save-agent version $SAVE_CLOUD_VERSION with config $config")
    logDebug("Serializer: ${ContinueResponse.serializer()}")
    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(HeartbeatResponse::class) {
                subclass(NewJobResponse::class)
                subclass(ContinueResponse::class)
                subclass(WaitResponse::class)
            }
        }
    }
    val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
        }
    }
    val saveAgent = SaveAgent(config, httpClient)
    runBlocking {
        saveAgent.start()
    }
}
