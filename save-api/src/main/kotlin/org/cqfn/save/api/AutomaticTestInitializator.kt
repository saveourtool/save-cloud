package org.cqfn.save.api

import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Pass
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.testsuite.TestSuiteDto
import org.cqfn.save.utils.LocalDateTimeSerializer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import org.slf4j.LoggerFactory

import java.io.IOException
import java.time.LocalDateTime
import java.util.*

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

internal val json = Json {
    serializersModule = SerializersModule {
        polymorphic(TestResultDebugInfo::class)
        polymorphic(DebugInfo::class)
        polymorphic(Pass::class)
        contextual(LocalDateTime::class, LocalDateTimeSerializer)
    }
}

class AutomaticTestInitializator {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)
    private val httpClient = HttpClient(Apache) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        install(Auth) {
            basic {
                // by default, ktor will wait for the server to respond with 401,
                // and only then send the authentication header
                // therefore, adding sendWithoutRequest is required
                sendWithoutRequest { true }
                credentials {
                    BasicAuthCredentials(username = "admin", password = "")
                }
            }
        }
    }

    @OptIn(InternalAPI::class)
    suspend fun start() {
        val configuration = readConfiguration()
        requireNotNull(configuration) {
            "Configuration couldn't be empty!"
        }

        log.info("\n\n-------------------Get all standard test suites---------------------")

        val response = httpClient.get<HttpResponse> {
            url("${configuration.backendUrl}/api/allStandardTestSuites")
            header("X-Authorization-Source", "basic")
            contentType(ContentType.Application.Json)
        }

        log.info("\n\n\n==========================\nStatus: ${response.status}\n")

        val result = response.receive<List<TestSuiteDto>>()
        log.info("Result: $result")

        log.info("-------------------Start execution---------------------")

        httpClient.post<HttpResponse> {
            url("${configuration.backendUrl}/api/submitExecutionRequest")
            header("X-Authorization-Source", "basic")
            body = MultiPartFormDataContent(formData {
                append("first_name", "John")
                append("last_name", "Doe")
                // append("document", file.readBytes(), Headers.build {
                // append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                // })
            })
        }
    }

    private fun readConfiguration(configFileName: String = "config.properties"): ConfigProperties? {
        try {
            val properties = Properties()
            val input = AutomaticTestInitializator::class.java.classLoader.getResourceAsStream(configFileName)
            if (input == null) {
                log.error("Unable to find configuration file: $configFileName")
                return null
            }
            properties.load(input)
            return ConfigProperties(
                properties.getProperty("backendUrl"),
                properties.getProperty("preprocessorUrl"),
            )
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
    }

    /**
     * @property backendUrl
     * @property preprocessorUrl
     */
    private data class ConfigProperties(
        val backendUrl: String,
        val preprocessorUrl: String,
    )
}


//
//        log.info("-------------------Start post debug info---------------------")
//
//        val testResultDebugInfo  = TestResultDebugInfo(
//            TestResultLocation(
//                "stub",
//                "stub",
//                "stub",
//                "stub",
//            ),
//            DebugInfo(
//                "stub",
//                "stub",
//                "stub",
//                1
//            ),
//            Pass(
//               "ok",
//               "ok",
//            )
//        )
//        httpClient.post<HttpResponse> {
//            url("${BACKEND_URL}/internal/files/debug-info?agentId=test-agent-id")
//            contentType(ContentType.Application.Json)
//            body = testResultDebugInfo
//        }
//
//
//        val testExecutionDto = TestExecutionDto(
//            filePath = "stub",
//            pluginName = "stub",
//            agentContainerId = "stub",
//            status = TestResultStatus.PASSED,
//            startTimeSeconds = LocalDateTime.parse("2021-01-01 00:03:07.000").toEpochSecond(ZoneOffset.UTC),
//            endTimeSeconds = LocalDateTime.parse("2021-01-01 00:03:44.000").toEpochSecond(ZoneOffset.UTC),
//            testSuiteName= null,
//            tags= emptyList(),
//            missingWarnings = 0,
//            matchedWarnings = 0,
//        )
