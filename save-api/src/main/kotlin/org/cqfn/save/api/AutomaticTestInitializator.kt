package org.cqfn.save.api

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.cqfn.save.agent.TestExecutionDto
import org.cqfn.save.core.result.DebugInfo
import org.cqfn.save.core.result.Pass
import org.cqfn.save.domain.TestResultDebugInfo
import org.cqfn.save.domain.TestResultLocation
import org.cqfn.save.domain.TestResultStatus
import org.cqfn.save.testsuite.TestSuiteDto
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

// TODO move into properties file
private const val BACKEND_URL = "http://localhost:5000"
private const val PREPROCESSOR_URL = "http://localhost:5200"


internal val json = Json {
    serializersModule = SerializersModule {
        polymorphic(TestResultDebugInfo::class)
        polymorphic(DebugInfo::class)
        polymorphic(Pass::class)
    }
}

class AutomaticTestInitializator {
    private val log = LoggerFactory.getLogger(AutomaticTestInitializator::class.java)

    @OptIn(InternalAPI::class)
    suspend fun start() {
        val httpClient = HttpClient(Apache) {
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
                    sendWithoutRequest { request ->
                        //request.url.host == "localhost"
                        true
                    }
                    credentials {
                        BasicAuthCredentials(username = "admin", password = "")
                    }
                }
            }
        }


        log.info("\n\n-------------------Get all standard test suites---------------------")

        val response = httpClient.get<HttpResponse> {
            url("${BACKEND_URL}/api/allStandardTestSuites")
            //header("Authorization", "Basic ${Base64.getEncoder().encodeToString("admin:".toByteArray())}")
            headers[HttpHeaders.Authorization] = "Basic ${Base64.getEncoder().encodeToString("admin:".toByteArray())}"
            contentType(ContentType.Application.Json)
        }

        val result = Json.decodeFromString<List<TestSuiteDto>>(response.toString())

        log.info("Status: ${response.status}\nContent: ${response.content}\nResult: ${result}")

        log.info("-------------------Start execution---------------------")

        httpClient.submitForm<HttpResponse> (
            url = "${BACKEND_URL}/submitExecutionRequest",
            formParameters = Parameters.build {
                append("first_name", "John")
                append("last_name", "Doe")
            }
        )


    }
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